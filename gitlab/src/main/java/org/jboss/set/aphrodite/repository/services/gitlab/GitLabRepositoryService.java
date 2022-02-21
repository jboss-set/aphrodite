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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;

/**
 * <p>Repository service for gitlab. It uses the <a href="https://github.com/gitlab4j/gitlab4j-api">
 * gitlabapi</a> to communicate with the server.</p>
 *
 * @author rmartinc
 */
public class GitLabRepositoryService extends AbstractRepositoryService implements RepositoryService {

    private static final Log LOG = LogFactory.getLog(GitLabRepositoryService.class);

    private GitLabApi gitLabApi;
    private GitLabPullRequestHomeService prHome;

    /**
     * Empty constructor.
     */
    public GitLabRepositoryService() {
        super(RepositoryType.GITLAB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init(RepositoryConfig config) {
        boolean result = super.init(config);
        LOG.debug("Initializing GitLab repository " + config.getUrl());
        if (result) {
            try {
                // TODO: Try using username/password too
                gitLabApi = new GitLabApi(config.getUrl(), config.getPassword());
                // get the current user and check the name
                User user = gitLabApi.getUserApi().getCurrentUser();
                if (user.getUsername().equalsIgnoreCase(config.getUsername())) {
                    this.prHome = new GitLabPullRequestHomeService(gitLabApi, this);
                    result = true;
                } else {
                    LOG.warn("Username is different to the configuration one " + user.getName());
                    result = false;
                }
            } catch (GitLabApiException e) {
                LOG.warn("Error initializing gitlab " + config.getUrl(), e);
                result = false;
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Log getLog() {
        return LOG;
    }

    // repository

    private Repository getRepository(String repoId) throws NotFoundException {
        try {
            List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(repoId);
            Repository repo = new Repository(new URL(baseUrl + repoId));
            List<Codebase> branchNames = new ArrayList<>(branches.size());
            for (Branch b : branches) {
                branchNames.add(new Codebase(b.getName()));
            }
            repo.getCodebases().addAll(branchNames);
            return repo;
        } catch (GitLabApiException|MalformedURLException e) {
            throw new NotFoundException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Repository getRepository(URL url) throws NotFoundException {
        GitLabUtils.checkIsInRepo(url, baseUrl);

        String repoId = GitLabUtils.getProjectIdFromURL(url);
        if (repoId == null) {
            throw new NotFoundException("Repository " + url + " cannot be found.");
        }
        return getRepository(repoId);
    }

    // Pull Request

    /**
     * {@inheritDoc}
     */
    @Override
    public PullRequest getPullRequest(URL url) throws NotFoundException {
        GitLabUtils.checkIsInRepo(url, baseUrl);

        String[] res = GitLabUtils.getProjectIdAndLastFieldFromURL(url);
        if (res != null && res.length == 2) {
            try {
                String repoId = res[0];
                int mergeId = Integer.parseInt(res[1]);
                Repository repo = getRepository(repoId);
                MergeRequest merge = gitLabApi.getMergeRequestApi().getMergeRequest(repoId, mergeId);
                List<Commit> commits = gitLabApi.getMergeRequestApi().getCommits(repoId, mergeId);
                return GitLabUtils.toPullRequest(merge, commits, url, repo, prHome);
            } catch (GitLabApiException e) {
                throw new NotFoundException(e);
            }
        }
        throw new NotFoundException("Merge Request " + url + " cannot be found.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PullRequest> getPullRequestsByState(Repository repository, PullRequestState state) throws NotFoundException {
        String repoId = GitLabUtils.getProjectIdFromURL(repository.getURL());
        try {
            Project project = gitLabApi.getProjectApi().getProject(repoId);
            MergeRequestFilter filter = new MergeRequestFilter();
            filter.setState(Constants.MergeRequestState.OPENED);
            filter.setProjectId(project.getId());
            List<MergeRequest> merges = gitLabApi.getMergeRequestApi().getMergeRequests(filter);
            List<PullRequest> prs = new ArrayList<>(merges.size());
            for (MergeRequest merge : merges) {
                List<Commit> commits = gitLabApi.getMergeRequestApi().getCommits(repoId, merge.getIid());
                prs.add(GitLabUtils.toPullRequest(merge, commits, new URL(repository.getURL() + "/merge_requests/" + merge.getIid()), repository, prHome));
            }
            return prs;
        } catch (GitLabApiException|MalformedURLException e) {
            throw new NotFoundException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public List<PullRequest> findPullRequestsRelatedTo(PullRequest pullRequest) {
        return prHome.findReferencedPullRequests(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public List<PullRequest> getPullRequestsAssociatedWith(Issue issue) throws NotFoundException {
        // not implementeed in github
        Utils.logException(LOG, new UnsupportedOperationException("Not yet implemented."));
        return Collections.emptyList();
    }

    // Comments

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void addCommentToPullRequest(PullRequest pullRequest, String comment) throws NotFoundException {
        prHome.addComment(pullRequest, comment);
    }

    // Labels methods

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasModifiableLabels(Repository repository) throws NotFoundException {
        String repoId = GitLabUtils.getProjectIdFromURL(repository.getURL());
        try {
            User user = gitLabApi.getUserApi().getCurrentUser();
            Member member = gitLabApi.getProjectApi().getMember(repoId, user.getId());
            return member.getAccessLevel().ordinal() >= AccessLevel.DEVELOPER.ordinal();
        } catch (GitLabApiException e) {
            LOG.info("hasModifiableLabels error", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException {
        String repoId = GitLabUtils.getProjectIdFromURL(repository.getURL());
        try {
            List<org.gitlab4j.api.models.Label> labels = gitLabApi.getLabelsApi().getProjectLabels(repoId);
            List<Label> res = new ArrayList<>(labels.size());
            for (org.gitlab4j.api.models.Label l : labels) {
                res.add(GitLabUtils.toLabel(l, repository.getURL()));
            }
            return res;
        } catch (GitLabApiException e) {
            throw new NotFoundException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public List<Label> getLabelsFromPullRequest(PullRequest pullRequest) throws NotFoundException {
        return prHome.getLabels(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void setLabelsToPullRequest(PullRequest pullRequest, List<Label> labels) throws NotFoundException {
        prHome.setLabels(pullRequest, labels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void addLabelToPullRequest(PullRequest pullRequest, String labelName) throws NotFoundException {
        prHome.addLabel(pullRequest, new Label(labelName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void removeLabelFromPullRequest(PullRequest pullRequest, String labelName) throws NotFoundException {
        prHome.removeLabel(pullRequest, new Label(labelName));
    }

    // commit status

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public CommitStatus getCommitStatusFromPullRequest(PullRequest pullRequest) throws NotFoundException {
        return prHome.getCommitStatus(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean repositoryAccessable(URL url) {
        try {
            String repoId = GitLabUtils.getProjectIdFromURL(url);
            boolean res = GitLabUtils.urlIsInRepo(url, baseUrl) &&
                gitLabApi.getRepositoryApi().getBranches(repoId) != null;
            return res;
        } catch (GitLabApiException e) {
            LOG.info("Invalid repo url " + url, e);
            return false;
        }
    }

    @Override
    public List<org.jboss.set.aphrodite.domain.Commit> getCommitsSince(URL url, String branch, long since) {
        try {
            String repoId = GitLabUtils.getProjectIdFromURL(url);
            List<Commit> glCommits = gitLabApi.getCommitsApi().getCommits(repoId, branch, new Date(since), new Date());

            List<org.jboss.set.aphrodite.domain.Commit> commits = new ArrayList<>();
            for (Commit c : glCommits) {
                commits.add(new org.jboss.set.aphrodite.domain.Commit(c.getId(), c.getTitle()));
            }
            return commits;
        } catch (GitLabApiException glae) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimit getRateLimit() throws NotFoundException {
        // TODO: nothing here
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryType getRepositoryType() {
        return repositoryType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PullRequestHome getPullRequestHome() {
        return prHome;
    }
}