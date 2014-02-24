package org.jboss.pull.shared.connectors.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.jboss.pull.shared.Util;

public class GithubHelper {

    private final String GITHUB_ORGANIZATION;
    private final String GITHUB_REPO;
    private final String GITHUB_LOGIN;
    private final String GITHUB_TOKEN;

    private final IRepositoryIdProvider repository;
    private final CommitService commitService;
    private final IssueService issueService;
    private final PullRequestService pullRequestService;
    private final MilestoneService milestoneService;

    private final Properties props;

    public GithubHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {
            props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

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

        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }

    }

    public PullRequest getPullRequest(int id) throws IOException {
        return getPullRequest(repository, id);
    }

    public PullRequest getPullRequest(String upstreamOrganization, String upstreamRepository, int id) throws IOException {
        return getPullRequest(RepositoryId.create(upstreamOrganization, upstreamRepository), id);
    }

    public PullRequest getPullRequest(IRepositoryIdProvider repository, int id) throws IOException {
        return pullRequestService.getPullRequest(repository, id);
    }

    public List<PullRequest> getPullRequests(String state) {
        List<PullRequest> result;
        try {
            result = pullRequestService.getPullRequests(repository, state);
        } catch (IOException e) {
            System.err.printf("Couldn't get pull requests in state %s of repository %s due to %s.\n", state, repository, e);
            result = new ArrayList<PullRequest>();
        }
        return result;
    }

    public List<Comment> getPullRequestComments(int pullNumber) {
        List<Comment> result;
        try {
            result = issueService.getComments(repository, pullNumber);
        } catch (IOException e) {
            System.err.printf("Couldn't get comments of pull request #%d due to %s.\n", pullNumber, e);
            result = new ArrayList<Comment>();
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
            System.err.printf("Problem posting a status build for sha: %s\n", pull.getHead().getSha());
            e.printStackTrace(System.err);
        }
    }

    public void postGithubComment(PullRequest pull, String comment) {
        try {
            issueService.createComment(repository, pull.getNumber(), comment);
        } catch (IOException e) {
            System.err.printf("Problem posting a comment build for pull: %d\n", pull.getNumber());
            e.printStackTrace(System.err);
        }
    }

    public List<Milestone> getMilestones() {
        List<Milestone> milestones;
        try {
            milestones = milestoneService.getMilestones(repository, "open");
        } catch (IOException e) {
            System.err.printf("Problem getting milestones");
            e.printStackTrace(System.err);
            milestones = new ArrayList<Milestone>();
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
            System.err.printf("Problem creating new milestone. title: " + title);
            e.printStackTrace(System.err);
        }
        return returnMilestone;
    }

    public org.eclipse.egit.github.core.Issue getIssue(int id) {
        org.eclipse.egit.github.core.Issue issue = null;
        try {
            issue = issueService.getIssue(repository, id);
        } catch (IOException e) {
            System.err.printf("Problem getting issue. id: " + id);
            e.printStackTrace(System.err);
        }
        return issue;
    }

    public org.eclipse.egit.github.core.Issue editIssue(org.eclipse.egit.github.core.Issue issue) {
        org.eclipse.egit.github.core.Issue returnIssue = null;
        try {
            returnIssue = issueService.editIssue(repository, issue);
        } catch (IOException e) {
            System.err.printf("Problem editing issue. id: " + issue.getId());
            e.printStackTrace(System.err);
        }
        return returnIssue;
    }

    public List<Comment> getComments(PullRequest pull) throws IOException{
        return issueService.getComments(repository, pull.getNumber());
    }

    public String getGithubLogin(){
        return GITHUB_LOGIN;
    }

    public boolean isMerged(PullRequest pull) throws IOException{
        return pullRequestService.isMerged(pull.getBase().getRepo(), pull.getNumber());
    }

}
