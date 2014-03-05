package org.jboss.pull.shared.connectors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.bugzilla.Bug;
import org.jboss.pull.shared.connectors.github.GithubHelper;

public class RedhatPullRequest {

    private static final Pattern BUGZILLA_ID_PATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)",
            Pattern.CASE_INSENSITIVE);

    // This has to match two patterns
    // * https://github.com/uselessorg/jboss-eap/pull/4
    // * https://api.github.com/repos/uselessorg/jboss-eap/pulls/4
    private static final Pattern PULLREQUEST_URL_PATTERN = Pattern.compile(
            ".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);

    private PullRequest pullRequest;
    private List<Bug> bugs = new ArrayList<Bug>();
    private List<RedhatPullRequest> relatedPullRequests = new ArrayList<RedhatPullRequest>();;

    // private PullHelper helper;
    private BZHelper bzHelper;
    private GithubHelper ghHelper;

    public RedhatPullRequest(PullRequest pullRequest, BZHelper bzHelper, GithubHelper ghHelper) {
        this.pullRequest = pullRequest;
        this.bzHelper = bzHelper;
        this.ghHelper = ghHelper;

        bugs = getBugsFromDescription(pullRequest);
        relatedPullRequests = getPRFromDescription();

    }

    private List<RedhatPullRequest> getPRFromDescription() {
        Matcher matcher = PULLREQUEST_URL_PATTERN.matcher(getGithubDescription());

        List<RedhatPullRequest> relatedPullRequests = new ArrayList<RedhatPullRequest>();
        while (matcher.find()) {
            PullRequest relatedPullRequest = ghHelper.getPullRequest(matcher.group(1), matcher.group(2), Integer.valueOf(matcher.group(3)));
            if( relatedPullRequest != null){
                relatedPullRequests.add(new RedhatPullRequest(relatedPullRequest, bzHelper, ghHelper));
            }
        }

        return relatedPullRequests;
    }

    private List<Bug> getBugsFromDescription(PullRequest pull) {
        final List<Integer> ids = checkBugzillaId(pull.getBody());
        final ArrayList<Bug> bugs = new ArrayList<Bug>();

        for (Integer id : ids) {
            final Bug bug = bzHelper.getBug(id);
            if (bug != null) {
                bugs.add(bug);
            }
        }
        return bugs;
    }

    private List<Integer> checkBugzillaId(String body) {
        final ArrayList<Integer> ids = new ArrayList<Integer>();
        final Matcher matcher = BUGZILLA_ID_PATTERN.matcher(body);
        while (matcher.find()) {
            try {
                ids.add(Integer.valueOf(matcher.group(1)));
            } catch (NumberFormatException ignore) {
                System.err.printf("Invalid bug number: %s.\n", ignore);
            }
        }
        return ids;
    }

    public int getNumber() {
        return pullRequest.getNumber();
    }

    public void postGithubComment(String comment) {
        ghHelper.postGithubComment(pullRequest, comment);
    }

    public Milestone getMilestone() {
        return pullRequest.getMilestone();
    }

    public void setMilestone(Milestone milestone) {
        Issue issue = ghHelper.getIssue(pullRequest);

        issue.setMilestone(milestone);
        ghHelper.editIssue(issue);
    }

    public String getTargetBranchTitle() {
        return pullRequest.getBase().getRef();
    }

    public String getSourceBranchSha() {
        return pullRequest.getHead().getSha();
    }

    public User getGithubUser() {
        return pullRequest.getUser();
    }

    public List<Comment> getGithubComments() {
        return ghHelper.getComments(pullRequest);
    }

    public void postGithubStatus(String targetUrl, String status) {
        ghHelper.postGithubStatus(pullRequest, targetUrl, status);
    }

    public String getGithubDescription() {
        return pullRequest.getBody();
    }

    public Date getGithubUpdatedAt() {
        return pullRequest.getUpdatedAt();
    }

    public Comment getLastMatchingGithubComment(Pattern pattern) {
        return ghHelper.getLastMatchingComment(pullRequest, pattern);
    }

    public List<RedhatPullRequest> getRelatedPullRequests() {
        return relatedPullRequests;
    }

    public String getState() {
        return pullRequest.getState();
    }

    public String getHtmlUrl() {
        return pullRequest.getHtmlUrl();
    }

    public boolean isMerged() {
        return ghHelper.isMerged(pullRequest);
    }

    public List<Bug> getBugs() {
        return bugs;
    }

    public BuildResult getBuildResult() {
        BuildResult buildResult = BuildResult.UNKNOWN;
        Comment comment = ghHelper.getLastMatchingComment(pullRequest, PullHelper.BUILD_OUTCOME);

        if (comment != null) {
            Matcher matcher = PullHelper.BUILD_OUTCOME.matcher(comment.getBody());
            while (matcher.find()) {
                buildResult = BuildResult.valueOf(matcher.group(2));
            }
        }

        return buildResult;
    }

    public String getOrganization() {
        Matcher matcher = PULLREQUEST_URL_PATTERN.matcher(pullRequest.getUrl());
        if( matcher.matches() ){
            return matcher.group(1);
        }
        return null;
    }

    public String getRepository() {
        Matcher matcher = PULLREQUEST_URL_PATTERN.matcher(pullRequest.getUrl());
        if( matcher.matches() ){
            return matcher.group(2);
        }
        return null;
    }

    public boolean updateBugzillaStatus(Bug bug, Bug.Status status){
        return bzHelper.updateBugzillaStatus(bug.getId(), status);
    }

}
