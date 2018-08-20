/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewBuilder;
import org.kohsuke.github.GHPullRequestReviewEvent;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.createRepositoryIdFromUrl;
import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.getPRFromDescription;

import static org.jboss.set.aphrodite.repository.services.github.GithubUtils.getCombineStatus;

/**
 * Service implementation of {@link PullRequestHome}. This helps to detach pull request specific methods in
 * {@link GitHubRepositoryService} and allow to call them from pull request itself once this service implementation registered
 * in container.
 */
public class GithubPullRequestHomeService extends AbstractGithubService implements PullRequestHome {
    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.repository.services.github.GithubPullRequestHomeService.class);
    private static final GitHubWrapper WRAPPER = new GitHubWrapper();

    public GithubPullRequestHomeService(Aphrodite aphrodite) {
        super(RepositoryType.GITHUB);
        AphroditeConfig configuration = aphrodite.getConfig();
        this.init(configuration);
    }

    @Override
    public List<PullRequest> findReferencedPullRequests(PullRequest pullRequest) {
        try {
            List<URL> urls = getPRFromDescription(pullRequest.getURL(), pullRequest.getBody());
            List<PullRequest> referencedPullRequests = new ArrayList<>();
            for (URL url : urls) {
                // Only try and retrieve pull request if it is located on the same host as this service
                if (url.getHost().equals(baseUrl.getHost())) {
                    PullRequest validPullRequest = getPullRequest(url);
                    if (validPullRequest != null) {
                        referencedPullRequests.add(getPullRequest(url));
                    }
                } else {
                    Utils.logWarnMessage(LOG, "Unable to process url '" + url + "' as it is not located on this service");
                }
            }
            return referencedPullRequests;
        } catch (MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "error to get referenced pull requests to " + pullRequest.getURL(), e);
            return Collections.emptyList();
        }
    }

    private PullRequest getPullRequest(URL url) {
        String[] elements = url.getPath().split("/");
        int pullId = Integer.parseInt(elements[elements.length - 1]);
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest pullRequest = repository.getPullRequest(pullId);
            return WRAPPER.pullRequestToPullRequest(pullRequest);
        } catch (IOException e) {
            Utils.logException(LOG, "Unable to retrieve pull request from url " + url, e);
            return null;
        }
    }

    @Override
    public boolean addComment(PullRequest pullRequest, String comment) {
        URL url = pullRequest.getURL();

        int id = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(id);
            issue.comment(comment);
            return true;
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
    }

    @Override
    public List<Label> getLabels(PullRequest pullRequest) {
        URL url = pullRequest.getURL();
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(Integer.parseInt(pullRequest.getId()));
            return WRAPPER.pullRequestLabeltoPullRequestLabel(issue.getLabels());
        } catch (IOException | NumberFormatException e) {
            Utils.logException(LOG, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean setLabels(PullRequest pullRequest, List<Label> labels) {
        URL url = pullRequest.getURL();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            List<GHLabel> issueLabels = new ArrayList<>();
            List<GHLabel> existingLabels = repository.listLabels().asList();

            for (Label label : labels) {
                GHLabel validLabel = validAndGetLabel(repository, label, existingLabels);
                if(validLabel != null) {
                    issueLabels.add(validLabel);
                }
            }
            List<String> list = issueLabels.stream().map(e -> e.getName()).collect(Collectors.toList());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean addLabel(PullRequest pullRequest, Label label) {
        URL url = pullRequest.getURL();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);

        try {
            GHRepository repository = github.getRepository(repositoryId);
            List<GHLabel> existingLabels = repository.listLabels().asList();
            GHLabel newLabel = validAndGetLabel(repository, label, existingLabels);
            if (newLabel == null) {
                Utils.logWarnMessage(LOG, "No label exists with name '" + label.getName() + "' at repository '" + repository.getName() + "'");
                return false;
            }
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();
            if (labels.contains(newLabel)) {
                return true; // label is already existed.
            }

            List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
            list.add(newLabel.getName());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        return true;
    }

    private GHLabel validAndGetLabel(GHRepository repository, Label label, List<GHLabel> existingLabels) throws IOException {
        for (GHLabel exsitingLabel : existingLabels) {
            if (exsitingLabel.getName().equalsIgnoreCase(label.getName()))
                return exsitingLabel;
        }
        return null;
    }

    @Override
    public boolean removeLabel(PullRequest pullRequest, Label label) {
        URL url = pullRequest.getURL();
        String labelName = label.getName();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);

        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();
            for (GHLabel l : labels)
                if (l.getName().equalsIgnoreCase(labelName)) {
                    // remove the label and reset
                    List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
                    list.remove(l.getName());
                    String[] labelArray = list.toArray(new String[list.size()]);
                    issue.setLabels(labelArray);
                    return true;
                }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        Utils.logWarnMessage(LOG, "No label exists with name '" + labelName + "' at repository '" + repositoryId + "'");
        return false;
    }

    @Override
    public CommitStatus getCommitStatus(PullRequest pullRequest) {
        URL url = pullRequest.getURL();
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
            return CommitStatus.UNKNOWN;
        }

        if (status != null) {
            return status;
        } else {
            return CommitStatus.UNKNOWN;
        }
    }

    @Override
    public void approveOnPullRequest(PullRequest pullRequest) {
        // if we set to null, it will actually set "" to comment
        createSimplePullRequestReview(pullRequest, GHPullRequestReviewEvent.APPROVE, "");
    }

    @Override
    public void requestChangesOnPullRequest(PullRequest pullRequest, String body) {
        createSimplePullRequestReview(pullRequest, GHPullRequestReviewEvent.REQUEST_CHANGES, body);
    }

    private GHPullRequestReview findReviewStateByUser(PullRequest pullRequest, GHUser user) {
        URL url = pullRequest.getURL();
        int pullRequestId = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest ghPullRequest = repository.getPullRequest(pullRequestId);
            List<GHPullRequestReview> reviews = ghPullRequest.listReviews().asList();
            ListIterator<GHPullRequestReview> li = reviews.listIterator(reviews.size());
            // Iterate in reverse. created date and updated date are always Null, Is this really safe?
            while (li.hasPrevious()) {
                GHPullRequestReview review = li.previous();
                if (review.getUser().equals(user)) return review;
            }
        } catch (IOException e) {
            Utils.logException(LOG, e);
        }
        return null;
    }

    private void createSimplePullRequestReview(PullRequest pullRequest, GHPullRequestReviewEvent event, String body) {
        GHPullRequestReview review = findReviewStateByUser(pullRequest, user);
        if (review != null && skipReviewEvent(event, review.getState()) && review.getBody().equals(body)) {
            return; // skip if review state and comment is unchanged.
        }
        URL url = pullRequest.getURL();
        int pullRequestId = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);

        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest ghPullRequest = repository.getPullRequest(pullRequestId);
            GHPullRequestReviewBuilder builder = ghPullRequest.createReview();
            builder.event(event).body(body).create();
        } catch (IOException e) {
            Utils.logException(LOG, e);
        }
    }

    // hack for review state and event, conversion method is not exposed from github-api
    private boolean skipReviewEvent(GHPullRequestReviewEvent event, GHPullRequestReviewState state) {
        if (event.equals(GHPullRequestReviewEvent.APPROVE) && state.equals(GHPullRequestReviewState.APPROVED)) {
            return true;
        } else if (event.equals(GHPullRequestReviewEvent.REQUEST_CHANGES) && state.equals(GHPullRequestReviewState.CHANGES_REQUESTED)) {
            return true;
        } else if (event.equals(GHPullRequestReviewEvent.PENDING) && state.equals(GHPullRequestReviewState.PENDING)) {
            return true;
        } else if (event.equals(GHPullRequestReviewEvent.COMMENT) && state.equals(GHPullRequestReviewState.COMMENTED)) {
            return true;
        }
        return false;
    }

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
            Utils.logWarnMessage(LOG, "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return false;
        }
        return true;
    }

    @Override
    protected Log getLog() {
        return LOG;
    }
}
