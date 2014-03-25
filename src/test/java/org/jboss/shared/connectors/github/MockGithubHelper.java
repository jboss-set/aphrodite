package org.jboss.shared.connectors.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.jboss.pull.shared.connectors.github.GithubHelper;

public class MockGithubHelper extends GithubHelper {

    public MockGithubHelper() {
    }

    @Override
    public PullRequest getPullRequest(int id) {
        return getPullRequest(RepositoryId.create("default", "default"), id);
    }

    @Override
    public PullRequest getPullRequest(String upstreamOrganization, String upstreamRepository, int id) {
        return getPullRequest(RepositoryId.create(upstreamOrganization, upstreamRepository), id);
    }
    
    private PullRequest getPullRequest(IRepositoryIdProvider repository, int id) {
        PullRequest pullRequest = new PullRequest();
        pullRequest.setId(id);
        pullRequest.setBody("");
        
        return pullRequest;
    }
    
    public List<RepositoryBranch> getBranches(){
        return new ArrayList<RepositoryBranch>();
    }

    public void postGithubStatus(PullRequest pull, String targetUrl, String status) {
    }

    public void postGithubComment(PullRequest pull, String comment) {
        System.out.println("MOCK: postGithubComment");
    }

    public List<Milestone> getMilestones() {
        return new ArrayList<Milestone>();
    }

    public Milestone createMilestone(String title) {
        System.out.println("MOCK: createMilestone: ");
        return new Milestone();
    }

    public Issue getIssue(PullRequest pullRequest) {
        return new Issue();
    }

    private int getIssueIdFromIssueURL(String issueURL) {
        return 0;
    }

    public Issue editIssue(Issue issue) {
        return new Issue();
    }

    public String getGithubLogin() {
        return "";
    }

    public boolean isMerged(PullRequest pullRequest) {
        return false;
    }

    public Comment getLastMatchingComment(PullRequest pullRequest, Pattern pattern) {
        return new Comment();
    }

    public List<Comment> getComments(PullRequest pullRequest) {
        return new ArrayList<Comment>();
    }
}
