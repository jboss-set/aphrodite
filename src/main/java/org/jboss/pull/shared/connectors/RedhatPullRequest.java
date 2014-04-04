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

package org.jboss.pull.shared.connectors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.bugzilla.Bug;
import org.jboss.pull.shared.connectors.common.Issue;
import org.jboss.pull.shared.connectors.github.GithubHelper;
import org.jboss.pull.shared.connectors.jira.JiraHelper;
import org.jboss.pull.shared.connectors.jira.JiraIssue;

public class RedhatPullRequest {
    private PullRequest pullRequest;

    private List<Issue> bugs = null;
    private List<Issue> jiraIssues = null;
    private List<RedhatPullRequest> relatedPullRequests = null;

    private IssueHelper bzHelper;
    private IssueHelper jiraHelper;
    private GithubHelper ghHelper;

    public RedhatPullRequest(PullRequest pullRequest, IssueHelper bzHelper, IssueHelper jiraHelper,
                             GithubHelper ghHelper) {
        this.pullRequest = pullRequest;
        if (bzHelper instanceof BZHelper && jiraHelper instanceof JiraHelper) {
            this.bzHelper = bzHelper;
            this.jiraHelper = jiraHelper;
        } else {
            throw new IllegalArgumentException("The first IssueHelper parameter has to be an instance of BZHelper and" +
                    " the second IssueHelper parameter must be an instance of JiraHelper.");
        }

        this.ghHelper = ghHelper;

        this.bugs = getBugsFromDescription();
        this.jiraIssues = getJiraIssuesFromDescription();

        // Can't call getPRFromDescription here. If two PR's reference each other a loop occurs.
    }

    private List<Issue> getBugsFromDescription() {
        final List<URL> urls = extractURLs(Constants.BUGZILLA_BASE_ID, Constants.BUGZILLA_ID_PATTERN);
        final ArrayList<Issue> bugs = new ArrayList<Issue>();
        for (URL url: urls) {
            if (bzHelper.accepts(url)) {
                Bug bug = (Bug) bzHelper.findIssue(url);
                if (bug != null) {
                    bugs.add(bug);
                }
            }
        }
        return bugs;
    }

    private List<Issue> getJiraIssuesFromDescription() {
        final List<URL> urls = extractURLs(Constants.JIRA_BASE_BROWSE, Constants.RELATED_JIRA_PATTERN);
        final List<Issue> jiraIssues = new ArrayList<Issue>();
        for (URL url : urls) {
            if (jiraHelper.accepts(url)) {
                JiraIssue jiraIssue = (JiraIssue) jiraHelper.findIssue(url);
                jiraIssues.add(jiraIssue);
            }
        }
        return jiraIssues;
    }

    private List<URL> extractURLs(String urlBase, Pattern toMatch) {
        final List<URL> urls = new ArrayList<URL>();
        final Matcher matcher = toMatch.matcher(pullRequest.getBody());
        while (matcher.find()) {
            try {
                urls.add(new URL(urlBase + matcher.group(1)));
            } catch (NumberFormatException ignore) {
                System.err.printf("Invalid bug number: %s.\n", ignore);
            } catch (MalformedURLException malformed) {
                System.err.printf("Invalid URL formed: %s. \n", malformed);
            }
        }
        return urls;
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
        org.eclipse.egit.github.core.Issue issue = ghHelper.getIssue(pullRequest);

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

    /**
     * Searches for last github comment that contains the pattern.
     *
     * @param pattern - REGEX pattern to match against comment body.
     * @return Last comment that matches the pattern or null if no comments match.
     */
    public Comment getLastMatchingGithubComment(Pattern pattern) {
        return ghHelper.getLastMatchingComment(pullRequest, pattern);
    }

    /**
     * Returns true if PR link is in the description
     * @return
     */
    public boolean hasRelatedPullRequestInDescription() {
        if (relatedPullRequests != null) {
            return relatedPullRequests.size() > 0;
        }else {
            return (relatedPullRequests = getPRFromDescription()).size() > 0;
        }
    }

    public List<RedhatPullRequest> getRelatedPullRequests() {
        if (relatedPullRequests != null) {
            return relatedPullRequests;
        } else {
            return relatedPullRequests = getPRFromDescription();
        }
    }

    private List<RedhatPullRequest> getPRFromDescription() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(getGithubDescription());

        List<RedhatPullRequest> relatedPullRequests = new ArrayList<RedhatPullRequest>();
        while (matcher.find()) {
            PullRequest relatedPullRequest = ghHelper.getPullRequest(matcher.group(1), matcher.group(2),
                    Integer.valueOf(matcher.group(3)));
            if (relatedPullRequest != null) {
                relatedPullRequests.add(new RedhatPullRequest(relatedPullRequest, bzHelper, jiraHelper, ghHelper));
            }
        }

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

    /**
     * Returns a merged list of both Bugzilla and Jira Issues found in the body of a Pull Request.
     *
     * @return
     */
    public List<Issue> getIssues() {
        List<Issue> toReturn = new ArrayList<Issue>(bugs.size() + jiraIssues.size());
        toReturn.addAll(bugs);
        toReturn.addAll(jiraIssues);
        return toReturn;
    }

    /**
     * Returns true if BZ link is in the PR description
     *
     * @return
     */
    public boolean hasBZInDescription() {
        return bugs.size() > 0;
    }

    /**
     * Returns true if JIRA link is in the PR description
     *
     * @return
     */
    public boolean hasJiraInDescription() {
        return jiraIssues.size() > 0;
    }

    public boolean isUpstreamRequired() {
        return !Constants.UPSTREAM_NOT_REQUIRED.matcher(pullRequest.getBody()).find();
    }

    public BuildResult getBuildResult() {
        BuildResult buildResult = BuildResult.UNKNOWN;
        Comment comment = ghHelper.getLastMatchingComment(pullRequest, Constants.BUILD_OUTCOME);

        if (comment != null) {
            Matcher matcher = Constants.BUILD_OUTCOME.matcher(comment.getBody());
            while (matcher.find()) {
                buildResult = BuildResult.valueOf(matcher.group(2));
            }
        }

        return buildResult;
    }

    public String getOrganization() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(pullRequest.getUrl());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getRepository() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(pullRequest.getUrl());
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    public boolean updateStatus(Issue issue, Enum status) throws IllegalArgumentException {
        if (issue instanceof Bug) {
            // Do BZ stuff
            if (bzHelper.accepts(issue.getUrl())) return bzHelper.updateStatus(issue.getUrl(), status);
        } else if (issue instanceof JiraIssue) {
            // Do Jira stuff
            if (jiraHelper.accepts(issue.getUrl())) return jiraHelper.updateStatus(issue.getUrl(), status);
        } else {
            throw new IllegalArgumentException("Your issue implementation has to be an instance of a Bug or " +
                    "JiraIssue");
        }
        return false;
    }

    public boolean isGithubMilestoneNullOrDefault(){
        return (pullRequest.getMilestone() == null || pullRequest.getMilestone().getTitle().contains("x"));
    }

}
