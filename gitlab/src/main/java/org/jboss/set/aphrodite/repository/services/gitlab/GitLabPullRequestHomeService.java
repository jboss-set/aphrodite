/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.repository.services.gitlab;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatusFilter;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestParams;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * <p>PullRequestHome implementation for the gitlab repository</p>
 *
 * @author rmartinc
 */
public class GitLabPullRequestHomeService implements PullRequestHome {

    private static final Log LOG = LogFactory.getLog(GitLabPullRequestHomeService.class);

    private final GitLabApi gitLabApi;
    private final GitLabRepositoryService gitLabRepo;
    private final Pattern prPattern;

    /**
     * Constructor using the the api.
     * @param gitLabApi The api to query the server
     * @param gitLabRepo The gitlab repository
     */
    public GitLabPullRequestHomeService(GitLabApi gitLabApi, GitLabRepositoryService gitLabRepo) {
        this.gitLabApi = gitLabApi;
        this.gitLabRepo = gitLabRepo;
        // create a pattern to locate the PRs in the description to this GitLab repository
        String patternString = ".*" + Pattern.quote(gitLabRepo.getBaseUrl().getHost()) + ".*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/-?/?merge_requests/(\\d+)";
        prPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

    // comments

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addComment(PullRequest pullRequest, String comment) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            gitLabApi.getDiscussionsApi().createMergeRequestDiscussion(repoId, mergeId, comment, new Date(), null, null);
            return true;
        } catch (GitLabApiException e) {
            LOG.debug("addComment error", e);
            return false;
        }
    }

    // labels

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Label> getLabels(PullRequest pullRequest) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            MergeRequest merge = gitLabApi.getMergeRequestApi().getMergeRequest(repoId, mergeId);
            List<String> labels = merge.getLabels();
            List<Label> res = new ArrayList<>(labels.size());
            for (String name : labels) {
                org.gitlab4j.api.models.Label l = gitLabApi.getLabelsApi().getProjectLabel(repoId, name);
                res.add(GitLabUtils.toLabel(l, pullRequest.getRepository().getURL()));
            }
            return res;
        } catch (GitLabApiException e) {
            Utils.logException(LOG, "Error getting the labels", e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLabels(PullRequest pullRequest, List<Label> labels) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            List<String> names = labels.stream().map(Label::getName).collect(Collectors.toList());
            gitLabApi.getMergeRequestApi().updateMergeRequest(repoId, mergeId, new MergeRequestParams().withLabels(names));
            return true;
        } catch (GitLabApiException e) {
            Utils.logException(LOG, "Error setting the labels", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addLabel(PullRequest pullRequest, Label label) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            MergeRequest merge = gitLabApi.getMergeRequestApi().getMergeRequest(repoId, mergeId);
            List<String> names = merge.getLabels();
            if (!names.contains(label.getName())) {
                names.add(label.getName());
                gitLabApi.getMergeRequestApi().updateMergeRequest(repoId, mergeId, new MergeRequestParams().withLabels(names));
                return true;
            }
        } catch (GitLabApiException e) {
            Utils.logException(LOG, "Error adding the label", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeLabel(PullRequest pullRequest, Label label) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            MergeRequest merge = gitLabApi.getMergeRequestApi().getMergeRequest(repoId, mergeId);
            List<String> names = merge.getLabels();
            if (names.remove(label.getName())) {
                gitLabApi.getMergeRequestApi().updateMergeRequest(repoId, mergeId, new MergeRequestParams().withLabels(names));
                return true;
            }
        } catch (GitLabApiException e) {
            Utils.logException(LOG, "Error removing the label", e);
        }
        return false;
    }

    // Pull Requests

    private List<URL> getPRFromDescription(URL url, String content) throws MalformedURLException, URISyntaxException {
        Matcher gitlabMatcher = prPattern.matcher(content);
        List<URL> relatedPullRequests = new ArrayList<>();
        while (gitlabMatcher.find()) {
            if (gitlabMatcher.groupCount() == 3) {
                URL relatedPullRequest = new URI(gitLabRepo.getBaseUrl() + gitlabMatcher.group(1) + "/"
                        + gitlabMatcher.group(2) + "/-/merge_requests/" + gitlabMatcher.group(3)).toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        return relatedPullRequests;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PullRequest> findReferencedPullRequests(PullRequest pullRequest) {
        try {
            List<URL> urls = getPRFromDescription(pullRequest.getURL(), pullRequest.getBody());
            List<PullRequest> related = new ArrayList<>();
            for (URL url : urls) {
                if (GitLabUtils.urlIsInRepo(url, pullRequest.getRepository().getURL())) {
                    try {
                        related.add(gitLabRepo.getPullRequest(url));
                    } catch (NotFoundException e) {
                        Utils.logException(LOG, "Unable to retrieve url '" + url + "' referenced in the pull request.", e);
                    }
                }
            }
            return related;
        } catch (MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "something went wrong while trying to get related pull requests to " + pullRequest.getURL(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommitStatus getCommitStatus(PullRequest pullRequest) {
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            List<Commit> commits = pullRequest.getCommits();
            if (commits != null && !commits.isEmpty()) {
                // get the last commit
                String sha = commits.get(commits.size() - 1).getSha();
                CommitStatusFilter filter = new CommitStatusFilter().withAll(true);
                List<org.gitlab4j.api.models.CommitStatus> statuses = gitLabApi.getCommitsApi().getCommitStatuses(repoId, sha, filter);
                // TODO: there are no test suite here so what to do here, for the moment returning one (it's always empty right now)
                if (!statuses.isEmpty()) {
                    return GitLabUtils.toCommitStatus(statuses.iterator().next().getStatus());
                }
            }
            return CommitStatus.UNKNOWN;
        } catch (GitLabApiException e) {
            Utils.logException(LOG, e);
            return CommitStatus.UNKNOWN;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void approveOnPullRequest(PullRequest pullRequest) {
        // Approvals are only in gitlab enterprise (gitlab.cee.redhat.com has no approvals)
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            // TODO: not tested as gitlab.cee has no approvals
            gitLabApi.getMergeRequestApi().approveMergeRequest(repoId, mergeId, null);
        } catch (GitLabApiException e) {
            LOG.error("Error approving the request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestChangesOnPullRequest(PullRequest pullRequest, String body) {
        // Approvals are only in gitlab enterprise (gitlab.cee.redhat.com has no approvals)
        String repoId = GitLabUtils.getProjectIdFromURL(pullRequest.getRepository().getURL());
        int mergeId = Integer.parseInt(pullRequest.getId());
        try {
            // TODO: not tested as gitlab.cee has no approvals
            gitLabApi.getMergeRequestApi().unapproveMergeRequest(repoId, mergeId);
        } catch (GitLabApiException e) {
            Utils.logException(LOG, e);
        }
    }
}