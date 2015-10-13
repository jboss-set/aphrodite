/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.set.aphrodite.issue.trackers.jira;

import com.google.common.collect.Iterables;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.IssueLink;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.RestException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.*;

/**
 * An implementation of the <code>IssueTrackerService</code> for the JIRA issue tracker.
 *
 * @author Ryan Emerson
 */
public class JiraIssueTracker extends AbstractIssueTracker {

    private static final Log LOG = LogFactory.getLog(JiraIssueTracker.class);

    private final IssueWrapper WRAPPER = new IssueWrapper();
    private JiraClient jiraClient;

    public JiraIssueTracker() {
        super("jira");
    }

    @Override
    public boolean init(IssueTrackerConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        try {
            ICredentials credentials = new BasicCredentials(config.getUsername(), config.getPassword());
            jiraClient = new JiraClient(baseUrl.toString(), credentials);
            // Check if provided credentials are correct, an exception is thrown if they aren't
            jiraClient.getRestClient().get("/rest/api/2/myself");
        } catch (IOException | URISyntaxException e) {
            Utils.logException(LOG, e);
            return false;
        } catch (RestException e) {
            Utils.logException(LOG, "Authentication failed for IssueTrackerService: " + this.getClass().getName(), e);
            return false;
        }
        return true;
    }

    @Override
    public Issue getIssue(URL url) throws NotFoundException {
        checkHost(url);
        net.rcarz.jiraclient.Issue jiraIssue = getIssue(getIssueKey(url));
        return WRAPPER.jiraIssueToIssue(url, jiraIssue);
    }

    private net.rcarz.jiraclient.Issue getIssue(Issue issue) throws NotFoundException {
        String trackerId = issue.getTrackerId().orElse(getIssueKey(issue.getURL()));
        return getIssue(trackerId);
    }

    private net.rcarz.jiraclient.Issue getIssue(String trackerId) throws NotFoundException {
        try {
            return jiraClient.getIssue(trackerId);
        } catch (JiraException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        List<Issue> issues = new ArrayList<>();
        String jql = new JiraQueryBuilder(searchCriteria).getJQLString();

        int maxResults = searchCriteria.getMaxResults().orElse(JiraQueryBuilder.DEFAULT_MAX_RESULTS);
        try {
            net.rcarz.jiraclient.Issue.SearchResult sr = jiraClient.searchIssues(jql, maxResults);
            sr.issues.forEach(issue -> issues.add(WRAPPER.jiraSearchIssueToIssue(baseUrl, issue)));
        } catch (JiraException e) {
            Utils.logException(LOG, e);
        }
        return issues;
    }

    /**
     * Known limitations:
     * - Jira api does not allow an issue type to be update (WTF?)
     * - Jira api does not allow project to be changed
     */
    @Override
    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        checkHost(issue.getURL());

        try {
            net.rcarz.jiraclient.Issue jiraIssue = getIssue(issue);
            net.rcarz.jiraclient.Issue.FluentUpdate update = WRAPPER.issueToFluentUpdate(issue, jiraIssue.update());
            update.execute();
            if (!hasSameIssueStatus(issue, jiraIssue)) {
                String transition = getJiraTransition(issue, jiraIssue);
                jiraIssue.transition().execute(transition);
            }
            addNewIssueLinks(issue, jiraIssue);
            removeOldIssueLinks(issue, jiraIssue);
            return true;
        } catch (JiraException e) {
            throw new AphroditeException(getUpdateErrorMessage(issue, e), e);
        }
    }

    private void removeOldIssueLinks(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) throws JiraException, NotFoundException {
        Set<String> ids = new HashSet<>();
        for (URL url : Iterables.concat(issue.getBlocks(), issue.getDependsOn()))
            ids.add(getIssueKey(url));

        for (IssueLink link : jiraIssue.getIssueLinks()) {
            net.rcarz.jiraclient.Issue linkedIssue = link.getInwardIssue();
            if (linkedIssue == null)
                linkedIssue = link.getOutwardIssue();

            if (!ids.contains(linkedIssue.getKey()))
                link.delete();
        }
    }

    private void addNewIssueLinks(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) throws JiraException, NotFoundException {
        for (URL url : issue.getBlocks()) {
            String trackerId = getIssueKey(url);
            net.rcarz.jiraclient.Issue issueToBlock = getIssue(trackerId);
            if (!issueLinkExists(url, issueToBlock.getIssueLinks()))
                issueToBlock.link(jiraIssue.getKey(), "Dependency");
        }

        for (URL url : issue.getDependsOn()) {
            if (!issueLinkExists(url, jiraIssue.getIssueLinks())) {
                String trackerId = getIssueKey(url);
                jiraIssue.link(trackerId, "Dependency");
            }
        }
    }

    private boolean issueLinkExists(URL url, List<IssueLink> links) throws NotFoundException {
        Predicate<IssueLink> predicate = checkIfLinkExists(url);
        return links.stream().anyMatch(predicate);
    }

    private Predicate<IssueLink> checkIfLinkExists(URL url) throws NotFoundException {
        String trackerId = getIssueKey(url);
        return p -> {
            if (p.getInwardIssue() != null)
                return p.getInwardIssue().getKey().equals(trackerId);

            return p.getOutwardIssue() != null && p.getOutwardIssue().getKey().equals(trackerId);
        };
    }

    @Override
    public boolean addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        super.addCommentToIssue(issue, comment);

        if (comment.isPrivate())
            Utils.logWarnMessage(LOG, "Private comments are not currently supported by " + getClass().getName());

        try {
            // TODO make so that a comment is added directly to issue without retrieving issue first?
            getIssue(issue).addComment(comment.getBody());
            return true;
        } catch (JiraException e) {
            Utils.logException(LOG, e);
            return false;
        }
    }

    @Override
    public Log getLog() {
        return LOG;
    }

    private String getIssueKey(URL url) throws NotFoundException {
        String path = url.getPath();
        boolean api = path.contains(API_ISSUE_PATH);
        boolean browse = path.contains(BROWSE_ISSUE_PATH);

        if (!(api || browse))
            throw new NotFoundException("The URL path must be of the form '" + API_ISSUE_PATH +
                    "' OR '" + BROWSE_ISSUE_PATH + "'");

        return api ? path.substring(API_ISSUE_PATH.length()) : path.substring(BROWSE_ISSUE_PATH.length());
    }

    private String getUpdateErrorMessage(Issue issue, JiraException e) {
        String msg = e.getMessage();
        if (msg.contains("does not exist or read-only")) {
            for (Map.Entry<Flag, String> entry : FLAG_MAP.entrySet()) {
                if (msg.contains(entry.getValue())) {
                    String retMsg = "Flag '%1$s' set in Issue.stage cannot be set for %2$s '%3$s'";
                    return getOptionalErrorMessage(retMsg, issue.getProduct(), entry.getKey(), issue.getURL());
                }
            }
            if (msg.contains(TARGET_RELEASE)) {
                String retMsg = "Release.milestone cannot be set for %2$s ''%3$s'";
                return getOptionalErrorMessage(retMsg, issue.getProduct(), null, issue.getURL());
            }
        }
        return null;
    }

    private String getOptionalErrorMessage(String template, Optional<?> optional, Object val, URL url) {
        if (optional.isPresent())
            return String.format(template, val, "issues in project", optional.get());
        else
            return String.format(template, val, "issue at ", url);
    }
}
