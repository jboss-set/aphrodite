/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.pull.shared.connectors.github;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.jboss.pull.shared.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubHelper {
    private static Log LOG = LogFactory.getLog(GithubHelper.class);

    private final String GITHUB_ORGANIZATION;
    private final String GITHUB_REPO;
    private final String GITHUB_LOGIN;
    private final String GITHUB_TOKEN;

    private final IRepositoryIdProvider repository;

    private final CommitService commitService;
    private final IssueService issueService;
    private final PullRequestService pullRequestService;
    private final MilestoneService milestoneService;
    private final RepositoryService repositoryService;
    private final LabelService labelService;

    public GithubHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {
            Properties props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

            GITHUB_ORGANIZATION = Util.require(props, "github.organization");
            GITHUB_REPO = Util.require(props, "github.repo");

            GITHUB_LOGIN = Util.require(props, "github.login");
            GITHUB_TOKEN = Util.get(props, "github.token");

            GitHubClient client = new GitHubClient();
            if (GITHUB_TOKEN != null && GITHUB_TOKEN.length() > 0)
                client.setOAuth2Token(GITHUB_TOKEN);
            repository = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_REPO);
            commitService = new CommitService(client);
            issueService = new IssueService(client);
            pullRequestService = new PullRequestService(client);
            milestoneService = new MilestoneService(client);
            repositoryService = new RepositoryService(client);
            labelService = new LabelService(client);

        } catch (Exception e) {
            throw Util.logExceptionAndGet(LOG, e);
        }
    }

    List<RepositoryBranch> branches = null;

    public List<RepositoryBranch> getBranches() {
        if (branches == null) {
            branches = new ArrayList<RepositoryBranch>();
            try {
                branches = repositoryService.getBranches(repository);
            } catch (IOException e) {
                Util.logException(LOG, "Error retrieving branches from repository: ", e);
            }
        }
        return branches;
    }

    public PullRequest getPullRequest(int id) {
        return getPullRequest(repository, id);
    }

    public PullRequest getPullRequest(String upstreamOrganization, String upstreamRepository, int id) {
        return getPullRequest(RepositoryId.create(upstreamOrganization, upstreamRepository), id);
    }

    private PullRequest getPullRequest(IRepositoryIdProvider repository, int id) {
        PullRequest pullRequest = null;
        try {
            pullRequest = pullRequestService.getPullRequest(repository, id);
        } catch (IOException e) {
            Util.logException(LOG, "Coouldn't retrieve Pull Request with id: " + id + " from repsoitory " + repository.generateId(), e);
        }
        return pullRequest;
    }

    public List<PullRequest> getPullRequests(String state) {
        List<PullRequest> result = new ArrayList<>();
        try {
            result = pullRequestService.getPullRequests(repository, state);
        } catch (IOException e) {
            String message = String.format("Couldn't get pull requests in state %s of repository %s.", state, repository);
            Util.logException(LOG, message, e);
        }
        return result;
    }

    public void postGithubStatus(PullRequest pull, String targetUrl, String status) {
        try {
            CommitStatus commitStatus = new CommitStatus();
            commitStatus.setTargetUrl(targetUrl);
            commitStatus.setState(status);
            commitService.createStatus(repository, pull.getHead().getSha(), commitStatus);
        } catch (Exception e) {
            Util.logException(LOG, "Error posting a status for sha: " + pull.getHead().getSha(), e);
        }
    }

    public void postGithubComment(PullRequest pull, String comment) {
        try {
            issueService.createComment(repository, pull.getNumber(), comment);
        } catch (IOException e) {
            Util.logException(LOG, "Error posting a comment to pull request: " + pull.getNumber(), e);
        }
    }

    private List<Milestone> milestones = null;

    public List<Milestone> getMilestones() {
        if (milestones == null) {
            milestones = new ArrayList<>();
            try {
                milestones = milestoneService.getMilestones(repository, "open");
                milestones.addAll(milestoneService.getMilestones(repository, "closed"));
            } catch (IOException e) {
                Util.logException(LOG, "Error retrieving milestones", e);
            }
        }
        return milestones;
    }

    public Milestone createMilestone(String title) {
        Milestone newMilestone = new Milestone();
        newMilestone.setTitle(title);
        Milestone returnMilestone = null;
        try {
            returnMilestone = milestoneService.createMilestone(repository, newMilestone);
        } catch (IOException e) {
            Util.logException(LOG, "Problem creating new milestone with title: " + title, e);
        }
        return returnMilestone;
    }

    public Issue getIssue(PullRequest pullRequest) {
        int id = getIssueIdFromIssueURL(pullRequest.getIssueUrl());
        Issue issue = null;
        try {
            issue = issueService.getIssue(repository, id);
        } catch (IOException e) {
            Util.logException(LOG, "Problem getting issue with id: " + id, e);
        }
        return issue;
    }

    private int getIssueIdFromIssueURL(String issueURL) {
        return Integer.valueOf(issueURL.substring(issueURL.lastIndexOf("/") + 1));
    }

    public Issue editIssue(Issue issue) {
        Issue returnIssue = null;
        try {
            returnIssue = issueService.editIssue(repository, issue);
        } catch (IOException e) {
            Util.logException(LOG, "Problem editing issue with id: " + issue.getId(), e);
        }
        return returnIssue;
    }

    public String getGithubLogin() {
        return GITHUB_LOGIN;
    }

    public boolean isMerged(PullRequest pullRequest) {
        if (pullRequest == null) {
            return false;
        }

        if (!pullRequest.getState().equals("closed")) {
            return false;
        }

        try {
            if (pullRequestService.isMerged(pullRequest.getBase().getRepo(), pullRequest.getNumber())) {
                return true;
            }
        } catch (IOException e) {
            Util.logException(LOG, "Cannot get Merged information of the pull request with id: " + pullRequest.getNumber(), e);
        }

        try {
            final List<Comment> comments = issueService.getComments(pullRequest.getBase().getRepo(), pullRequest.getNumber());
            for (Comment comment : comments) {
                if (comment.getBody().toLowerCase().indexOf("merged") != -1) {
                    return true;
                }
            }
        } catch (IOException e) {
            Util.logException(LOG, "Cannot get comments of the pull request with id: " + pullRequest.getNumber(), e);
        }

        return false;
    }

    public Comment getLastMatchingComment(PullRequest pullRequest, Pattern pattern) {
        Comment lastComment = null;
        List<Comment> comments = getComments(pullRequest);

        for (Comment comment : comments) {
            Matcher matcher = pattern.matcher(comment.getBody());
            if (matcher.find()) {
                lastComment = comment;
            }
        }

        return lastComment;
    }

    public List<Comment> getComments(PullRequest pullRequest) {
        try {
            return issueService.getComments(repository, pullRequest.getNumber());
        } catch (IOException e) {
            Util.logException(LOG, "Error to get comments for pull request: " + pullRequest.getNumber(), e);
        }
        return new ArrayList<Comment>();
    }

    public List<Label> getLabels(PullRequest pullRequest) {
        Issue issue = getIssue(pullRequest);
        if (issue != null) {
            return issue.getLabels();
        }
        return new ArrayList<Label>();
    }

    public Label getLabel(final String title) {
        try {
            final String label = title.replace(" ", "%20");
            return labelService.getLabel(repository, label);
        } catch (IOException e) {
            Util.logException(LOG, "Error trying to get lavel: " + title, e);
        }
        return null;
    }

    public void addLabel(PullRequest pullRequest, Label label) {

        Issue issue = getIssue(pullRequest);

        List<Label> labels = issue.getLabels();
        labels.add(label);
        issue.setLabels(labels);

        editIssue(issue);

    }

    public void removeLabel(PullRequest pullRequest, Label newLabel) {

        Issue issue = getIssue(pullRequest);

        List<Label> labels = issue.getLabels();
        for( Label label : issue.getLabels() ){
            if( label.getName().equals(newLabel.getName())){
                labels.remove(label);
                break;
            }
        }
        issue.setLabels(labels);

        editIssue(issue);

    }
}
