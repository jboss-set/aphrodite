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

package org.jboss.set.aphrodite.issue.trackers.common;

import org.apache.commons.logging.Log;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An abstract IssueTracker which provides logic common to all issue trackers.
 *
 * @author Ryan Emerson
 */
public abstract class AbstractIssueTracker implements IssueTrackerService {
    public static final Pattern URL_REGEX = Pattern
            .compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?");

    protected final TrackerType TRACKER_TYPE;
    protected ExecutorService executorService;
    protected IssueTrackerConfig config;
    protected URL baseUrl;

    protected abstract Log getLog();

    public AbstractIssueTracker(TrackerType TRACKER_TYPE) {
        this.TRACKER_TYPE = TRACKER_TYPE;
    }

    @Override
    public boolean init(AphroditeConfig aphroditeConfig) {
        executorService = aphroditeConfig.getExecutorService();

        Iterator<IssueTrackerConfig> i = aphroditeConfig.getIssueTrackerConfigs().iterator();
        while (i.hasNext()) {
            IssueTrackerConfig config = i.next();
            if (config.getTracker() == TRACKER_TYPE) {
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
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            String errorMsg = "Invalid IssueTracker url. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(getLog(), errorMsg, e);
            return false;
        }
        return true;
    }

    @Override
    public List<Issue> getIssuesAssociatedWith(Patch patch) {
        List<Issue> issues = new ArrayList<>();
        Matcher m = URL_REGEX.matcher(patch.getTitle() + patch.getBody());
        while (m.find()) {
            String link = m.group();
            try {
                URL url = new URL(link);
                if (url.getHost().equals(baseUrl.getHost()))
                    issues.add(getIssue(url));
            } catch (MalformedURLException e) {
                if (getLog().isTraceEnabled())
                    getLog().trace(e);
            } catch (NotFoundException e) {
                Utils.logException(getLog(), "Unable to retrieve Issue at " + link + ":", e);
            }
        }
        return issues;
    }

    @Override
    public void addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        checkHost(issue.getURL());
        comment.getId().ifPresent(id ->
                Utils.logWarnMessage(getLog(), "ID: " + id + "ignored when posting comments " +
                        "as this is set by the issue tracker.")
        );
    }

    protected void checkHost(URL url) throws NotFoundException {
        if (!urlExists(url))
            throw new NotFoundException("The requested entity cannot be found at this tracker as " +
                    "the specified host domain is different from this service.");
    }

    @Override
    public boolean urlExists(URL url) {
        Objects.requireNonNull(url);
        return url.getHost().equals(baseUrl.getHost());
    }

    protected Map<Issue, Comment> filterIssuesByHost(Map<Issue, Comment> commentMap) {
        Objects.requireNonNull(commentMap);

        return commentMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null && urlExists(entry.getKey().getURL()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Collection<Issue> filterIssuesByHost(Collection<Issue> issues) {
        Objects.requireNonNull(issues);

        return issues.stream()
                .filter(i -> i != null && urlExists(i.getURL()))
                .collect(Collectors.toList());
    }

    protected Collection<URL> filterUrlsByHost(Collection<URL> urls) {
        Objects.requireNonNull(urls);

        return urls.stream()
                .filter(url -> url != null && urlExists(url))
                .collect(Collectors.toList());
    }
}
