/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.createRepositoryIdFromUrl;
import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.getPRFromDescription;

import static org.jboss.set.aphrodite.repository.services.github.GithubUtils.getCombineStatus;

/**
 * @author Ryan Emerson
 */
public class GitHubRepositoryService extends AbstractGithubService implements RepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.spi.RepositoryService.class);
    private static final GitHubWrapper WRAPPER = new GitHubWrapper();

    public GitHubRepositoryService() {
        super(RepositoryType.GITHUB);
    }

    @Override
    protected Log getLog() {
        return LOG;
    }

    @Override
    public PullRequest getPullRequest(URL url) throws NotFoundException {
        checkHost(url);

        String[] elements = url.getPath().split("/");
        int pullId = Integer.parseInt(elements[elements.length - 1]);
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest pullRequest = repository.getPullRequest(pullId);
            return WRAPPER.pullRequestToPullRequest(pullRequest);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public Repository getRepository(URL url) throws NotFoundException {
        checkHost(url);

        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            Collection<GHBranch> branches = repository.getBranches().values();
            return WRAPPER.toAphroditeRepository(url, branches);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

//    @Override
//    public List<PullRequest> getPullRequestsAssociatedWith(Issue issue) throws NotFoundException {
//        String trackerId = issue.getTrackerId().orElseThrow(() -> new IllegalArgumentException("Issue.trackerId must be set."));
//        try {
//            GitHubGlobalSearchService searchService = new GitHubGlobalSearchService(gitHubClient);
//            List<SearchResult> searchResults = searchService.searchAllPullRequests(trackerId);
//            return searchResults.stream()
//                    .map(pr -> getPullRequest(pr.getUrl()))
//                    .filter(patch -> patch != null)
//                    .collect(Collectors.toList());
//        } catch (IOException e) {
//            Utils.logException(LOG, e);
//            throw new NotFoundException(e);
//        }
//    }

    @Override
    @Deprecated
    public List<PullRequest> getPullRequestsAssociatedWith(Issue issue) throws NotFoundException {
        Utils.logException(LOG, new UnsupportedOperationException("Not yet implemented."));
        return Collections.emptyList();
    }

    @Override
    public List<PullRequest> getPullRequestsByState(Repository repository, PullRequestState state) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            // String githubState = state.toString().toLowerCase();
            GHRepository githubRepository = github.getRepository(repositoryId);
            GHIssueState issueState;
            try {
                issueState = GHIssueState.valueOf(state.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                issueState = GHIssueState.OPEN;
            }
            List<GHPullRequest> pullRequests = githubRepository.getPullRequests(issueState);
            return WRAPPER.toAphroditePullRequests(pullRequests);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    @Deprecated
    public void addCommentToPullRequest(PullRequest pullRequest, String comment) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);

        int id = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(id);
            issue.comment(comment);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public boolean hasModifiableLabels(Repository repository) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHMyself myself = github.getMyself();
            GHRepository githubRepository = github.getRepository(repositoryId);
            Set<GHUser> collaborators = githubRepository.listCollaborators().asSet();
            return collaborators.stream().anyMatch(e -> e.getLogin().equals(myself.getLogin()));
        } catch (Throwable t) {
            if (t.getMessage().contains("Must have push access")) {
                return false;
            }
            Exception e = (Exception) t;
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    @Deprecated
    public void addLabelToPullRequest(PullRequest pullRequest, String labelName) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);

        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHLabel newLabel = getLabel(repository, labelName);
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();
            if (labels.contains(newLabel)) {
                return;
            }

            List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
            list.add(newLabel.getName());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    private GHLabel getLabel(GHRepository repository, String labelName) throws NotFoundException, IOException {
        List<GHLabel> labels = repository.listLabels().asList();
        return getLabel(repository, labelName, labels);
    }

    private GHLabel getLabel(GHRepository repository, String labelName, List<GHLabel> validLabels) throws NotFoundException {
        for (GHLabel label : validLabels) {
            if (label.getName().equalsIgnoreCase(labelName))
                return label;
        }
        throw new NotFoundException("No label exists with the name '" + labelName +
                "' at repository '" + repository.getName() + "'");
    }

    @Override
    public List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createRepositoryIdFromUrl(url);
        List<GHLabel> labels;
        try {
            GHRepository githubRepository = github.getRepository(repositoryId);
            labels = githubRepository.listLabels().asList();
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }

        return WRAPPER.pullRequestLabeltoPullRequestLabel(labels);
    }

    @Override
    @Deprecated
    public List<Label> getLabelsFromPullRequest(PullRequest pullRequest) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(Integer.parseInt(pullRequest.getId()));
            return WRAPPER.pullRequestLabeltoPullRequestLabel(issue.getLabels());
        } catch (IOException | NumberFormatException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    @Deprecated
    public void setLabelsToPullRequest(PullRequest pullRequest, List<Label> labels) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);

        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            List<GHLabel> issueLabels = new ArrayList<>();
            List<GHLabel> existingLabels = repository.listLabels().asList();

            for (Label label : labels) {
                issueLabels.add(getLabel(repository, label.getName(), existingLabels));
            }
            List<String> list = issueLabels.stream().map(e -> e.getName()).collect(Collectors.toList());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    @Deprecated
    public void removeLabelFromPullRequest(PullRequest pullRequest, String name) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);

        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();

            for (GHLabel label : labels)
                if (label.getName().equalsIgnoreCase(name)) {
                    // remove the label and reset
                    List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
                    list.remove(label.getName());
                    String[] labelArray = list.toArray(new String[list.size()]);
                    issue.setLabels(labelArray);
                    return;
                }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
        throw new NotFoundException("No label exists with the name '" + name +
                "' at repository '" + repositoryId + "'");
    }

    @Override
    @Deprecated
    public List<PullRequest> findPullRequestsRelatedTo(PullRequest pullRequest) {
        try {
            List<URL> urls = getPRFromDescription(pullRequest.getURL(), pullRequest.getBody());
            List<PullRequest> related = new ArrayList<>();
            for (URL url : urls) {
                try {
                    // Only try and retrieve pull request if it is located on the same host as this service
                    if (urlExists(url)) {
                        related.add(getPullRequest(url));
                    } else {
                        Utils.logWarnMessage(LOG, "Unable to process url '" + url + "' as it is not located on this service");
                    }
                } catch (NotFoundException e) {
                    Utils.logException(LOG, "Unable to retrieve url '" + url + "' referenced in the pull request at: " + pullRequest.getURL(), e);
                }
            }
            return related;
        } catch (MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "something went wrong while trying to get related pull requests to " + pullRequest.getURL(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Deprecated
    public CommitStatus getCommitStatusFromPullRequest(PullRequest pullRequest) throws NotFoundException {
        URL url = pullRequest.getURL();
        checkHost(url);

        CommitStatus status = null;
        int pullRequestId = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            String sha = null;

            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest ghPullRequest = repository.getPullRequest(pullRequestId);

            List<GHPullRequestCommitDetail> commits = ghPullRequest.listCommits().asList();
            if (commits.size() > 0) {
                sha = commits.get(commits.size() - 1).getSha();
            }

            // statuses contains Finished and Started TeamCity Build
            List<GHCommitStatus> statuses = repository.listCommitStatuses(sha).asList();
            if (statuses.size() > 0) {
                GHCommitState sta = getCombineStatus(statuses);
                if (sta != null)
                    status = CommitStatus.fromString(sta.toString());
            }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }

        if (status != null) {
            return status;
        } else {
            return CommitStatus.UNKNOWN;
        }
    }

    @Override
    public boolean repositoryAccessable(URL url) {
        if (url.toString().contains("svn.jboss.org")) {
            // svn repository is not supported
            Utils.logWarnMessage(LOG, "svn repository : " + url + " is not supported.");
            return false;
        }

        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            repository.getBranches(); // action to test account repository accessibility
        } catch (IOException e) {
            Utils.logWarnMessage(LOG,
                    "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return false;
        }
        return true;
    }

    public RateLimit getRateLimit() throws NotFoundException {
        try {
            GHRateLimit ghRateLimit = github.getRateLimit();
            return WRAPPER.ghRateLimittoRateLimit(ghRateLimit);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public RepositoryType getRepositoryType() {
        return REPOSITORY_TYPE;
    }
}
