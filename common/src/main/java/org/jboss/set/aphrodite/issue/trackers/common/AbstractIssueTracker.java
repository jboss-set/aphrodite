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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * An abstract IssueTracker which provides logic common to all issue trackers.
 *
 * @author Ryan Emerson
 */
public abstract class AbstractIssueTracker implements IssueTrackerService {
    public static final Pattern URL_REGEX = Pattern
            .compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?\\d+");

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIssueTracker.class);

    protected final TrackerType TRACKER_TYPE;
    protected ExecutorService executorService;
    protected IssueTrackerConfig config;
    protected URI baseUrl;

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
            baseUrl = new URI(url);
        } catch (URISyntaxException e) {
            String errorMsg = "Invalid IssueTracker url. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(LOG, errorMsg, e);
            return false;
        }
        return true;
    }

    @Override
    public List<Issue> getIssuesAssociatedWith(PullRequest pullRequest) {
        List<Issue> issues = new ArrayList<>();
        Matcher m = URL_REGEX.matcher(pullRequest.getTitle() + pullRequest.getBody());
        while (m.find()) {
            String link = m.group();
            try {
                URI uri = new URI(link);
                if (uri.getHost().equals(baseUrl.getHost()))
                    issues.add(getIssue(uri));
            } catch (URISyntaxException e) {
                if (LOG.isTraceEnabled())
                    LOG.trace(e.getMessage(), e);
            } catch (NotFoundException e) {
                Utils.logException(LOG, "Unable to retrieve Issue at " + link + ":", e);
            }
        }
        return issues;
    }

    @Override
    public void addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        checkHost(issue.getURI());
        comment.getId().ifPresent(id ->
                Utils.logWarnMessage(LOG, "ID: " + id + "ignored when posting comments " +
                        "as this is set by the issue tracker.")
        );
    }

    protected void checkHost(URI uri) throws NotFoundException {
        if (!uriExists(uri))
            throw new NotFoundException("The requested entity cannot be found at this tracker as " +
                    "the specified host domain is different from this service.");
    }

    @Override
    public boolean uriExists(URI uri) {
        Objects.requireNonNull(uri);
        return convertToTrackerID(uri).equals(getTrackerID());
    }

    @Override
    public String getTrackerID() {
        return convertToTrackerID(this.baseUrl);
    }

    public static boolean exists(AbstractIssueTracker abstractIssueTracker) {
        return abstractIssueTracker.TRACKER_TYPE != null && abstractIssueTracker.baseUrl != null;
    }

    public static String convertToTrackerID(URI uri) {
        Objects.requireNonNull(uri);
        StringBuilder stringBuilder = new StringBuilder(40);
        stringBuilder.append(uri.getScheme()).append("://").append(uri.getHost());
        if(uri.getPort()>0)
            stringBuilder.append(":").append(uri.getPort());
        return stringBuilder.toString();
    }

    protected Map<Issue, Comment> filterIssuesByHost(Map<Issue, Comment> commentMap) {
        Objects.requireNonNull(commentMap);

        return commentMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null && uriExists(entry.getKey().getURI()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Collection<Issue> filterIssuesByHost(Collection<Issue> issues) {
        Objects.requireNonNull(issues);

        return issues.stream()
                .filter(i -> i != null && uriExists(i.getURI()))
                .collect(Collectors.toList());
    }

    protected Collection<URI> filterUrlsByHost(Collection<URI> uris) {
        Objects.requireNonNull(uris);

        return uris.stream()
                .filter(Predicate.not(Objects::isNull))
                .filter(this::uriExists)
                .collect(toList());
    }
}