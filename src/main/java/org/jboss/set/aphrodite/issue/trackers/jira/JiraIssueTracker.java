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
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.config.TrackerType;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.API_AUTHENTICATION_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.API_FILTER_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.API_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.COMMENT_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.FLAG_MAP;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.TARGET_RELEASE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getJiraTransition;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.hasSameIssueStatus;

/**
 * An implementation of the <code>IssueTrackerService</code> for the JIRA issue tracker.
 *
 * @author Ryan Emerson
 */
public class JiraIssueTracker extends AbstractIssueTracker {

    private static final Log LOG = LogFactory.getLog(JiraIssueTracker.class);
    private static final Pattern FILTER_NAME_PARAM_PATTERN = Pattern.compile("filter=([^&]+)");

    private final int MAX_THREADS = 10; // TODO expose this in the tracker's configuration
    private final IssueWrapper WRAPPER = new IssueWrapper();
    private JiraClient jiraClient;

    public JiraIssueTracker() {
        super(TrackerType.JIRA);
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
            jiraClient.getRestClient().get(API_AUTHENTICATION_PATH);
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
        String jql = new JiraQueryBuilder(searchCriteria).getJQLString();
        int maxResults = searchCriteria.getMaxResults().orElse(config.getDefaultIssueLimit());
        return searchIssues(jql, maxResults);
    }

    public List<Issue> searchIssues(String jql, int maxResults) {
        List<Issue> issues = new ArrayList<>();
        try {
            net.rcarz.jiraclient.Issue.SearchResult sr = jiraClient.searchIssues(jql, maxResults);
            sr.issues.forEach(issue -> issues.add(WRAPPER.jiraSearchIssueToIssue(baseUrl, issue)));
        } catch (JiraException e) {
            Utils.logException(LOG, e);
        }
        return issues;
    }

    @Override
    public List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException {
        String jql = getJQLFromFilter(filterUrl);
        return searchIssues(jql, config.getDefaultIssueLimit());
    }

    private String getJQLFromFilter(URL filterUrl) throws NotFoundException {
        String filterId = Utils.getParamaterFromUrl(FILTER_NAME_PARAM_PATTERN, filterUrl);
        try {
            JSON jsonResponse = jiraClient.getRestClient().get(API_FILTER_PATH + filterId);
            JSONObject jsonObject = (JSONObject) jsonResponse;
            return (String) jsonObject.get("jql");
        } catch (IOException | RestException | URISyntaxException e) {
            throw new NotFoundException("Unable to retrieve filter with id:=" + filterId, e);
        }
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
        return postComment(issue, comment, false);
    }

    private boolean postComment(Issue issue, Comment comment, boolean parallelRequest) throws NotFoundException {
        if (comment.isPrivate())
            Utils.logWarnMessage(LOG, "Private comments are not currently supported by " + getClass().getName());

        String trackerId = issue.getTrackerId().orElse(getIssueKey(issue.getURL()));
        String restURI = API_ISSUE_PATH + trackerId + COMMENT_ISSUE_PATH;
        JSONObject req = new JSONObject();
        req.put("body", comment.getBody());
        try {
            // A new JiraClient is created here to allow for comments to be posted in parallel
            // The underlying JiraClient is not thread-safe, hence it is necessary to create a new
            // JiraClient object for each request.
            if (parallelRequest) {
                ICredentials credentials = new BasicCredentials(config.getUsername(), config.getPassword());
                new JiraClient(baseUrl.toString(), credentials).getRestClient().post(restURI, req);
            } else {
                jiraClient.getRestClient().post(restURI, req);
            }
        } catch (Exception ex) {
            throw new NotFoundException("Failed to add comment to issue " + issue.getURL(), ex);
        }
        return true;
    }

    @Override
    public boolean addCommentToIssue(Map<Issue, Comment> commentMap) {
        commentMap = filterIssuesByHost(commentMap);
        if (commentMap.isEmpty())
            return true;

        // Comments are sent in parallel in order to reduce latency
        int numberOfThreads = commentMap.size() > MAX_THREADS ? MAX_THREADS : commentMap.size();

        List<Callable<Boolean>> futures = new ArrayList<>();
        commentMap.forEach((issue, comment) ->
                futures.add(() -> {
                    try {
                        return postComment(issue, comment, true);
                    } catch (NotFoundException e) {
                        if (LOG.isWarnEnabled())
                            LOG.warn(e);
                        return false;
                    }
                }));

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        try {
            return executorService.invokeAll(futures)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .allMatch(result -> result.equals(true));
        } catch (InterruptedException e) {
            if (LOG.isWarnEnabled())
                LOG.warn(e);
            return false;
        } finally {
            executorService.shutdown();
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
