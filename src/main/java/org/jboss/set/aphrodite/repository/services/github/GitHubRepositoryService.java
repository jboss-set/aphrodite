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

package org.jboss.set.aphrodite.repository.services.github;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_ISSUES;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SEARCH;

/**
 * @author Ryan Emerson
 */
public class GitHubRepositoryService extends AbstractRepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.spi.RepositoryService.class);
    private final GitHubWrapper WRAPPER = new GitHubWrapper();
    private GitHubClient gitHubClient;

    public GitHubRepositoryService() {
        super("github");
    }

    @Override
    protected Log getLog() {
        return LOG;
    }

    @Override
    public boolean init(RepositoryConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        try {
            gitHubClient = GitHubClient.createClient(baseUrl.toString());
            gitHubClient.setCredentials(config.getUsername(), config.getPassword());
            new UserService(gitHubClient).getUser();
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for RepositoryService: " + this.getClass().getName(), e);
            return false;
        }
        return true;
    }

    private Patch getPatch(String url) {
        try {
            return getPatch(new URL(url));
        } catch (MalformedURLException | NotFoundException e) {
            return null;
        }
    }

    @Override
    public Patch getPatch(URL url) throws NotFoundException {
        checkHost(url);

        String[] elements = url.getPath().split("/");
        int pullId = Integer.parseInt(elements[elements.length - 1]);
        RepositoryId repositoryId = RepositoryId.createFromUrl(url);
        PullRequestService pullRequestService = new PullRequestService(gitHubClient);
        try {
            PullRequest pullRequest = pullRequestService.getPullRequest(repositoryId, pullId);
            return WRAPPER.pullRequestToPatch(pullRequest);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public Repository getRepository(URL url) throws NotFoundException {
        checkHost(url);

        RepositoryId id = RepositoryId.createFromUrl(url);
        RepositoryService rs = new RepositoryService(gitHubClient);
        try {
            List<RepositoryBranch> branches = rs.getBranches(id);
            return WRAPPER.toAphroditeRepository(url, branches);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public List<Patch> getPatchesAssociatedWith(Issue issue) throws NotFoundException {
        String trackerId = issue.getTrackerId().orElseThrow(() -> new IllegalArgumentException("Issue.trackerId must be set."));
        try {
            GitHubGlobalSearchService searchService = new GitHubGlobalSearchService(gitHubClient);
            List<SearchResult> searchResults = searchService.searchAllPullRequests(trackerId);
            return searchResults.stream()
                    .map(pr -> getPatch(pr.getUrl()))
                    .filter(patch -> patch != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public List<Patch> getPatchesByStatus(Repository repository, PatchStatus status) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        RepositoryId id = RepositoryId.createFromUrl(url);
        PullRequestService pullRequestService = new PullRequestService(gitHubClient);
        try {
            String githubStatus =  status.toString().toLowerCase();
            List<PullRequest> pullRequests = pullRequestService.getPullRequests(id, githubStatus);
            return WRAPPER.toAphroditePatches(pullRequests);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public void addCommentToPatch(Patch patch, String comment) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int pullId = Integer.parseInt(patch.getId());
        RepositoryId id = RepositoryId.createFromUrl(url);
        try {
            IssueService is = new IssueService(gitHubClient);
            is.createComment(id, pullId, comment);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }
}
