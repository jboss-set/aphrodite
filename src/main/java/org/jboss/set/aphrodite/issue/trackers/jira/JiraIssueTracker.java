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

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.RestException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.API_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;

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
    public List<Issue> getIssuesAssociatedWith(Patch patch) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Issue getIssue(URL url) throws NotFoundException {
        super.getIssue(url);

        try {
            net.rcarz.jiraclient.Issue jiraIssue = jiraClient.getIssue(getIssueKey(url));
            return WRAPPER.jiraIssueToIssue(url, jiraIssue);
        } catch (JiraException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        return null;
    }

    @Override
    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        return false;
    }

    @Override
    public boolean addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        return false;
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
}
