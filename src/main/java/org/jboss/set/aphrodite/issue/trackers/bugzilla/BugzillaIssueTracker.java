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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the <code>IssueTrackerService</code> for the Bugzilla issue tracker.
 *
 * @author Ryan Emerson
 */
public class BugzillaIssueTracker extends AbstractIssueTracker {

    private static final Log LOG = LogFactory.getLog(BugzillaIssueTracker.class);

    private BugzillaClient bzClient;

    public BugzillaIssueTracker() {
        super(TrackerType.BUGZILLA);
    }

    @Override
    public boolean init(IssueTrackerConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        try {
            bzClient = new BugzillaClient(baseUrl, config.getUsername(), config.getPassword(), executorService);
        } catch (IllegalStateException e) {
            Utils.logException(LOG, e);
            return false;
        }
        return true;
    }

    @Override
    public Issue getIssue(URL url) throws NotFoundException {
        checkHost(url);
        return bzClient.getIssueWithComments(url);
    }

    @Override
    public List<Issue> getIssues(Collection<URL> urls) {
        urls = filterUrlsByHost(urls);
        if (urls.isEmpty())
            return new ArrayList<>();

        return bzClient.getIssues(urls);
    }

    @Override
    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        return bzClient.searchIssues(searchCriteria, config.getDefaultIssueLimit());
    }

    @Override
    public List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException {
        checkHost(filterUrl);
        return bzClient.searchIssuesByFilter(filterUrl);
    }

    @Override
    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        checkHost(issue.getURL());
        return bzClient.updateIssue(issue);
    }

    @Override
    public boolean addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        super.addCommentToIssue(issue, comment);
        return bzClient.postComment(issue, comment);
    }

    @Override
    public boolean addCommentToIssue(Map<Issue, Comment> commentMap) {
        commentMap = filterIssuesByHost(commentMap);
        return commentMap.isEmpty() || bzClient.postComment(commentMap);
    }

    @Override
    public boolean addCommentToIssue(Collection<Issue> issues, Comment comment) {
        issues = filterIssuesByHost(issues);
        return issues.isEmpty() || bzClient.postComment(issues, comment);
    }

    public BugzillaClient getBzClient() {
        return bzClient;
    }

    @Override
    protected Log getLog() {
        return LOG;
    }
}
