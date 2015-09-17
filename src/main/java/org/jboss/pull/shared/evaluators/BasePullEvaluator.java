/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared.evaluators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.RedhatPullRequest;
import org.jboss.pull.shared.connectors.bugzilla.Bug;
import org.jboss.pull.shared.connectors.common.Issue;
import org.jboss.pull.shared.connectors.jira.JiraIssue;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * An abstract base evaluator which holds the target github branch.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class BasePullEvaluator implements PullEvaluator {

    private static Log LOG = LogFactory.getLog(BasePullEvaluator.class);

    public static final String EVALUATOR_PROPERTY = "evaluator";
    public static final String GITHUB_BRANCH_PROPERTY = "github.branch";
    public static final String GITHUB_ORGANIZAITON_UPSTREAM = "github.organization.upstream";
    public static final String GITHUB_REPOSITORY_UPSTREAM = "github.repo.upstream";
    public static final String GITHUB_BRANCH_UPSTREAM = "github.branch.upstream";
    public static final String ISSUE_FIX_VERSION = "issue.fix.version";

    private static final String NOT_REVIEWED_TAG = "Pull request has not been reviewed yet";

    protected PullHelper helper;

    protected String issueFixVersion;
    protected String githubBranch;
    protected String upstreamOrganization;
    protected String upstreamRepository;
    protected String upstreamBranch;

    @Override
    public void init(final PullHelper helper, final Properties configuration, final String version) {
        this.helper = helper;

        this.issueFixVersion = Util.require(configuration, version + "." + ISSUE_FIX_VERSION);
        this.githubBranch = Util.require(configuration, version + "." + GITHUB_BRANCH_PROPERTY);
        this.upstreamOrganization = Util.require(configuration, version + "." + GITHUB_ORGANIZAITON_UPSTREAM);
        this.upstreamRepository = Util.require(configuration, version + "." + GITHUB_REPOSITORY_UPSTREAM);
        this.upstreamBranch = Util.require(configuration, version + "." + GITHUB_BRANCH_UPSTREAM);

    }

    @Override
    public String getTargetBranch() {
        return githubBranch;
    }

    @Override
    public Result isMergeable(final RedhatPullRequest pull) {
        final Result mergeable;
        mergeable = isMarkedForMerge(pull);
        mergeable.and(isMergeableByUpstream(pull));
        return mergeable;
    }

    private Result isMarkedForMerge(RedhatPullRequest pullRequest) {
        final Result result = new Result(false);

        Comment comment = pullRequest.getLastMatchingGithubComment(Constants.MERGE);

        if (comment != null) {
            if (pullRequest.getGithubUpdatedAt().compareTo(comment.getCreatedAt()) <= 0
                    && helper.isAdminUser(comment.getUser().getLogin())) {
                result.setMergeable(true);
                result.addDescription("+ Pull request has been reviewed");
            }
        }

        if (!result.isMergeable())
            result.addDescription("- " + NOT_REVIEWED_TAG);

        return result;
    }

    @Override
    public boolean updateIssueAsMerged(final RedhatPullRequest pull) {
        final List<Issue> issues = (List<Issue>) getIssue(pull);

        if (issues.isEmpty()) {
            Util.logWarnMessage(LOG, "Couldn't update issue as merged because there are no issues related to PR #" + pull.getNumber());
            return false;
        }

        if (issues.size() > 1) {
            // since we are not sure what we are updating if multiple issues related to a single PR
            // we cannot do anything, it is better to leave it open then to close an issue wrongly
            Util.logWarnMessage(LOG, "Couldn't update issue as merged since there are more than one issues related to PR #" + pull.getNumber());
            Util.logWarnMessage(LOG, "Issues related to PR#" + pull.getNumber() + ":");
            for (Issue issue : issues) {
                String msg = "Type: " + issue.getClass().getSimpleName() + " | #" + issue.getNumber() +
                        " | Url: " + issue.getUrl();
                Util.logWarnMessage(LOG, msg);
            }
            return false;
        }

        final Issue issue = issues.get(0);
        if (issue instanceof Bug) {
            return pull.updateStatus(issue, Bug.Status.MODIFIED);
        } else if (issue instanceof JiraIssue) {
            return updateJiraAsMerged((JiraIssue) issue);
        } else {
            throw new IllegalStateException("unsupported type of an issue: " + issue.getClass().getName());
        }
    }

    @Override
    public List<? extends Issue> getIssue(final RedhatPullRequest pull) {
        return getBugsThatMatchFixVersion(pull); // default implementation at the moment
    }

    @Override
    public List<RedhatPullRequest> getUpstreamPullRequest(final RedhatPullRequest pullRequest) {
        final ArrayList<RedhatPullRequest> upstreamPulls = new ArrayList<RedhatPullRequest>();

        final List<RedhatPullRequest> relatedPullRequests = pullRequest.getRelatedPullRequests();

        for (RedhatPullRequest relatedPullRequest : relatedPullRequests) {
            if (upstreamOrganization.equals(relatedPullRequest.getOrganization())
                    && upstreamRepository.equals(relatedPullRequest.getRepository())
                    && upstreamBranch.equals(relatedPullRequest.getTargetBranchTitle()))
                upstreamPulls.add(relatedPullRequest);
        }

        return upstreamPulls;
    }

    protected Result isMergeableByUpstream(final RedhatPullRequest pull) {
        final Result mergeable = new Result(true);

        final List<RedhatPullRequest> upstreamPulls = getUpstreamPullRequest(pull);
        if (upstreamPulls.isEmpty()) {
            mergeable.setMergeable(false);
            mergeable.addDescription("- Missing any upstream pull request");
            return mergeable;
        }

        for (RedhatPullRequest pullRequest : upstreamPulls) {
            if (!pullRequest.isMerged()) {
                mergeable.setMergeable(false);
                mergeable.addDescription("- Upstream pull request #" + pullRequest.getNumber() + " has not been merged yet");
            }
        }

        if (mergeable.isMergeable())
            mergeable.addDescription("+ Upstream pull request is OK");

        return mergeable;
    }

    protected List<Bug> getBugsThatMatchFixVersion(RedhatPullRequest pullRequest) {
        List<Issue> issues = pullRequest.getIssues();

        final List<Bug> returnBugs = new ArrayList<Bug>();
        for (Issue i : issues) {
            // Check if it is a bug, then make a yucky cast.
            if (i instanceof Bug) {
                Bug bug = (Bug) i ;
                for (String target : bug.getTargetRelease()) {
                    if (target.equals(issueFixVersion)) {
                        returnBugs.add(bug);
                        break;
                    }
                }
            }
        }
        return returnBugs;
    }

    private boolean updateJiraAsMerged(final JiraIssue issue) {
        // TODO
        throw new IllegalStateException("jira has not been implemented yet");
    }

    protected List<JiraIssue> getJiraIssue(PullRequest pull) {
        // TODO
        throw new IllegalStateException("jira has not been implemented yet");
    }

}
