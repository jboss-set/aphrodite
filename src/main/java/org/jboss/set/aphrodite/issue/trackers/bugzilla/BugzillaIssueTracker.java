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
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of the <code>IssueTrackerService</code> for the Bugzilla issue tracker.
 *
 * @author Ryan Emerson
 */
public class BugzillaIssueTracker implements IssueTrackerService {

    private static final Log LOG = LogFactory.getLog(BugzillaIssueTracker.class);

    private final String TRACKER_TYPE = "bugzilla";
    private BugzillaClient bzClient;
    private IssueTrackerConfig config;
    private URL baseUrl;

    @Override
    public boolean init(AphroditeConfig aphroditeConfig) {
        Iterator<IssueTrackerConfig> i = aphroditeConfig.getIssueTrackerConfigs().iterator();
        while (i.hasNext()) {
            IssueTrackerConfig config = i.next();
            if (config.getTracker().equalsIgnoreCase(TRACKER_TYPE)) {
                i.remove(); // Remove so that this service cannot be instantiated twice
                return init(config);
            }
        }
        return false;
    }

    @Override
    public boolean init(IssueTrackerConfig config) {
        this.config = config;
        String url = config.getUrl();
        if (!url.endsWith("/"))
            url = url + "/";

        try {
            baseUrl = new URL(config.getUrl());
        } catch (MalformedURLException e) {
            String errorMsg = "Invalid tracker url '" + url + "'. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(LOG, errorMsg, e);
            return false;
        }

        try {
            bzClient = new BugzillaClient(baseUrl, config.getUsername(), config.getPassword());
        } catch (IllegalStateException e) {
            Utils.logException(LOG, e);
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
        if (!url.getHost().equals(baseUrl.getHost()))
            throw new NotFoundException("The requested issue cannot be found on this tracker as the " +
            "requested issue is not hosted on this server.");

        return bzClient.getIssue(url);
    }

    @Override
    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        return bzClient.searchIssues(searchCriteria);
    }

    @Override
    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        return bzClient.updateIssue(issue);
    }

    @Override
    public boolean addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        comment.getId().ifPresent(id ->
                        Utils.logWarnMessage(LOG, "ID: " + id + "ignored when posting comments " +
                                "as this is set by the issue tracker.")
        );
        return bzClient.postComment(issue, comment);
    }

    public BugzillaClient getBzClient() {
        return bzClient;
    }
}
