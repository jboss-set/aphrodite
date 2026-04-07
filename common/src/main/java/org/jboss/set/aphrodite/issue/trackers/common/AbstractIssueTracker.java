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
    public static final String REGEX = "(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?\\d+";
    public static final Pattern URL_REGEX = Pattern.compile(REGEX);

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIssueTracker.class);

    protected final TrackerType TRACKER_TYPE;
    protected ExecutorService executorService;
    protected IssueTrackerConfig config;
    protected List<URI> baseUris;

    public AbstractIssueTracker(TrackerType TRACKER_TYPE) {
        this.TRACKER_TYPE = TRACKER_TYPE;
        baseUris = new ArrayList<>();
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
        List<String> uris = config.getURIs();
        for (String uri : uris) {
            String newUri = uri;
            if (!newUri.endsWith("/"))
                newUri = newUri + "/";
            try {
                baseUris.add(new URI(newUri));
            } catch (URISyntaxException e) {
                String errorMsg = "Invalid IssueTracker url. " + this.getClass().getName() + " service for '" + newUri + "' cannot be started";
                Utils.logException(LOG, errorMsg, e);
                return false;
            }
        }
        return true;
    }

    public URI getURI () {
        return this.baseUris.get(0);
    }

    private URI cloneURI (URI original, URI synonym) throws URISyntaxException {
        URI updatedUri = new URI(
                original.getScheme(),
                original.getUserInfo(),
                original.getHost(),
                original.getPort(),
                synonym.getPath(),
                synonym.getQuery(),
                synonym.getFragment()
        );
        return updatedUri;
    }

    @Override
    public List<Issue> getIssuesAssociatedWith(PullRequest pullRequest) {
        URI realURI = baseUris.get(0);
        List<Issue> issues = new ArrayList<>();
        for (URI baseUri : baseUris) {
            Matcher m = URL_REGEX.matcher(pullRequest.getTitle() + pullRequest.getBody());
            while (m.find()) {
                String synonym = m.group();
                try {
                    URI uri = cloneURI(realURI, new URI(synonym));
                    if (uri.getHost().equals(baseUri.getHost())) {
                        issues.add(getIssue(uri));
                    }
                } catch (URISyntaxException e) {
                    if (LOG.isTraceEnabled())
                        LOG.trace(e.getMessage(), e);
                } catch (NotFoundException e) {
                    Utils.logException(LOG, "Unable to retrieve Issue at " + synonym + ":", e);
                }
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
        return getTrackerID().contains(convertToTrackerID(uri));
    }

    @Override
    public List<String> getTrackerID() {
        return this.baseUris.stream().map(AbstractIssueTracker::convertToTrackerID).toList();
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