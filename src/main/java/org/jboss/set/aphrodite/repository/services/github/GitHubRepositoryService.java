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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;
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
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author Ryan Emerson
 */
public class GitHubRepositoryService extends AbstractRepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.spi.RepositoryService.class);
    private final GitHubWrapper WRAPPER = new GitHubWrapper();
    private GitHubClient gitHubClient;

    public GitHubRepositoryService() {
        super(RepositoryType.GITHUB);
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
            String githubStatus = status.toString().toLowerCase();
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

    @Override
    public void addLabelToPatch(Patch patch, String labelName) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int patchId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        RepositoryId repositoryId = RepositoryId.createFromUrl(url);
        IssueService issueService = new IssueService(gitHubClient);
        try {
            Label newLabel = getLabel(repositoryId, labelName);
            org.eclipse.egit.github.core.Issue issue = issueService.getIssue(repositoryId, patchId);
            List<Label> issueLabels = issue.getLabels();
            if (issueLabels.contains(newLabel))
                return;

            issueLabels.add(newLabel);
            issue.setLabels(issueLabels);
            issueService.editIssue(repositoryId, issue);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    private Label getLabel(RepositoryId repositoryId, String labelName) throws NotFoundException, IOException {
        LabelService labelService = new LabelService(gitHubClient);
        List<Label> labels = labelService.getLabels(repositoryId);
        for (Label label : labels)
            if (label.getName().equalsIgnoreCase(labelName))
                return label;

        throw new NotFoundException("No label exists with the name '" + labelName +
                "' at repository '" + repositoryId + "'");
    }

    private static final Pattern RELATED_PR_PATTERN = Pattern.compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN = Pattern.compile("([a-zA-Z_0-9-//]*)#(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO = Pattern.compile("([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)#(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<Patch> findPatchesRelatedTo(Patch patch) throws NotFoundException {
        try {
            List<URL> urls = getPRFromDescription(patch.getURL(), patch.getBody());
            List<Patch> related = new ArrayList<Patch>();
            for(URL url : urls) {
                try {
                    related.add(getPatch(url));
                } catch (NotFoundException e) {
                    Utils.logException(LOG, url + " in patch related " + patch.getURL() + " is not a valid url", e);
                }
            }
            return related;
        } catch(MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "something went wrong while trying to get related patches to " + patch.getURL(), e);
            return Collections.emptyList();
        }
    }

    private List<URL> getPRFromDescription(URL url, String content) throws MalformedURLException, URISyntaxException {
        String []paths = url.getPath().split("/");
        Matcher matcher = RELATED_PR_PATTERN.matcher(content);
        List<URL> relatedPullRequests = new ArrayList<URL>();
        while(matcher.find()) {
            if (matcher.groupCount() == 3) {
                URL relatedPullRequest = new URI("https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pulls/" + matcher.group(3) ).toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        Matcher abbreviatedMatcher = ABBREVIATED_RELATED_PR_PATTERN.matcher(content);
        while (abbreviatedMatcher.find()) {
            String match = abbreviatedMatcher.group();
            Matcher abbreviatedExternalMatcher = ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO.matcher(match);
            if (abbreviatedExternalMatcher.find()) {
                if (abbreviatedExternalMatcher.groupCount() == 3) {
                    URL relatedPullRequest = new URI("https://github.com/"
                            + abbreviatedExternalMatcher.group(1) + "/"
                            + abbreviatedExternalMatcher.group(2) + "/pulls/"
                            + abbreviatedExternalMatcher.group(3) ).toURL();
                    relatedPullRequests.add(relatedPullRequest);
                    continue;
                }
            }

            if (abbreviatedMatcher.groupCount() == 2) {
                URL relatedPullRequest = new URI("https://github.com/" + paths[1] + "/" + paths[2] + "/" + "/pulls/" + abbreviatedMatcher.group(2)).toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        return relatedPullRequests;
    }
}
