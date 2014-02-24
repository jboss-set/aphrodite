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

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.bugzilla.Bug;
import org.jboss.pull.shared.connectors.bugzilla.Issue;
import org.jboss.pull.shared.connectors.jira.JiraIssue;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract base evaluator which holds the target github branch.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class BasePullEvaluator implements PullEvaluator {
    public static final Pattern BUGZILLA_ID_PATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);

    public static final String EVALUATOR_PROPERTY = "evaluator";
    public static final String GITHUB_BRANCH_PROPERTY = "github.branch";
    public static final String GITHUB_ORGANIZAITON_UPSTREAM = "github.organization.upstream";
    public static final String GITHUB_REPOSITORY_UPSTREAM = "github.repo.upstream";
    public static final String GITHUB_BRANCH_UPSTREAM = "github.branch.upstream";
    public static final String ISSUE_FIX_VERSION = "issue.fix.version";

    private static final String NOT_REVIEWED_TAG = "Pull request has not been reviewed yet";

    private Pattern UPSTREAM_PATTERN = null;

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

        final String repo = upstreamOrganization + "/" + upstreamRepository;
        UPSTREAM_PATTERN = Pattern.compile("(github\\.com/" + repo + "/pull/|" + repo + "#)(\\d+)", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String getTargetBranch() {
        return githubBranch;
    }

    @Override
    public Result isMergeable(final PullRequest pull) {
        final Result mergeable;
        mergeable = isReviewed(pull);
        mergeable.and(isMergeableByUpstream(pull));
        return mergeable;
    }

    private Result isReviewed(PullRequest pull) {
        final Result result = new Result(false);

        final List<Comment> comments = helper.getGHHelper().getPullRequestComments(pull.getNumber());
        for (Comment comment : comments) {
            if (PullHelper.MERGE.matcher(comment.getBody()).matches()) {
                System.out.printf("issue #%d updated at: %s\n", pull.getNumber(), Util.getTime(pull.getUpdatedAt()));
                System.out.printf("issue #%d reviewed at: %s\n", pull.getNumber(), Util.getTime(comment.getCreatedAt()));

                if (pull.getUpdatedAt().compareTo(comment.getCreatedAt()) <= 0
                        && helper.isAdminUser(comment.getUser().getLogin())) {
                    result.setMergeable(true);
                    result.addDescription("+ Pull request has been reviewed");
                    break;
                }
            }
        }

        if (! result.isMergeable())
            result.addDescription("- " + NOT_REVIEWED_TAG);

        return result;
    }

    public static boolean isReviewed(final Result result) {
        for (String description: result.getDescription()) {
            if (description.indexOf(NOT_REVIEWED_TAG) != -1)
                return false;
        }
        return true;
    }

    @Override
    public boolean updateIssueAsMerged(final PullRequest pull) {
        final List<Issue> issues = (List<Issue>) getIssue(pull);

        if (issues.isEmpty() || issues.size() > 1) {
            // since we are not sure what we are updating if multiple issues related to a single PR
            // we cannot do anything, it is better to leave it open then to close an issue wrongly
            System.err.printf("WARNING: Couldn't update the relevant issue as merged since there are more than one or none such issue(-s) related to PR#%d.\n", pull.getNumber());
            System.err.println("WARNING: related issues:");
            for (Issue issue : issues) {
                System.err.printf("WARNING: Type: %s, Number: %s, Url: %s\n", issue.getClass().getSimpleName(), issue.getNumber(), issue.getUrl());
            }
            return false;
        }

        final Issue issue = issues.get(0);
        if (issue instanceof Bug) {
            return updateBugzillaAsMerged((Bug) issue);
        } else if (issue instanceof JiraIssue) {
            return updateJiraAsMerged((JiraIssue) issue);
        } else {
            throw new IllegalStateException("unsupported type of an issue: " + issue.getClass().getName());
        }
    }

    @Override
    public List<? extends Issue> getIssue(final PullRequest pull) {
        return getBug(pull);    // default implementation at the moment
    }

    @Override
    public List<PullRequest> getUpstreamPullRequest(final PullRequest pull) {
        final ArrayList<PullRequest> upstreamPulls = new ArrayList<PullRequest>();

        final Matcher matcher = UPSTREAM_PATTERN.matcher(pull.getBody());
        while (matcher.find()) {
            final Integer id = Integer.valueOf(matcher.group(2));
            try {
                final PullRequest upstreamPull = helper.getGHHelper().getPullRequest(RepositoryId.create(upstreamOrganization, upstreamRepository), id);

                if (upstreamBranch.equals(upstreamPull.getBase().getRef()))
                    upstreamPulls.add(upstreamPull);

            } catch (IOException e) {
                System.err.printf("Couldn't get a pull request #%d of repository %s/%s due to %s.\n", id, upstreamOrganization, upstreamRepository, e);
            }
        }
        return upstreamPulls;
    }

    private boolean updateBugzillaAsMerged(final Bug bug) {
        boolean result = false;
        try {
            result = helper.getBZHelper().updateBugzillaStatus(bug.getId(), Bug.Status.MODIFIED);
        } catch (Exception e) {
            System.err.printf("Update of the status of bugzilla bz%d failed due to %s.\n", bug.getId(), e);
            System.err.printf("Retrying...\n");
            try {
                result = helper.getBZHelper().updateBugzillaStatus(bug.getId(), Bug.Status.MODIFIED);
            } catch (Exception ex) {
                System.err.printf("Update of the status of bugzilla bz%d failed again due to %s.\n", bug.getId(), ex);
            }
        }
        return result;
    }

    private boolean updateJiraAsMerged(final JiraIssue issue) {
        // TODO
        throw new IllegalStateException("jira has not been implemented yet");
    }

    protected Result isMergeableByUpstream(final PullRequest pull) {
        final Result mergeable = new Result(true);

        try {
            final List<PullRequest> upstreamPulls = getUpstreamPullRequest(pull);
            if (upstreamPulls.isEmpty()) {
                mergeable.setMergeable(false);
                mergeable.addDescription("- Missing any upstream pull request");
                return mergeable;
            }

            for (PullRequest pullRequest : upstreamPulls) {
                if (! helper.isMerged(pullRequest)) {
                    mergeable.setMergeable(false);
                    mergeable.addDescription("- Upstream pull request #" + pullRequest.getNumber() + " has not been merged yet");
                }
            }

            if (mergeable.isMergeable()) {
                mergeable.addDescription("+ Upstream pull request is OK");
            }

        } catch (Exception ignore) {
            System.err.printf("Cannot get an upstream pull request of the pull request %d: %s.\n", pull.getNumber(), ignore);
            ignore.printStackTrace(System.err);

            mergeable.setMergeable(false);
            mergeable.addDescription("Cannot get an upstream pull request of the pull request " + pull.getNumber() + ": " + ignore.getMessage());
        }

        return mergeable;
    }

    protected List<JiraIssue> getJiraIssue(PullRequest pull) {
        // TODO
        throw new IllegalStateException("jira has not been implemented yet");
    }

    protected List<Bug> getBug(PullRequest pull) {
        final List<Integer> ids = checkBugzillaId(pull.getBody());
        final ArrayList<Bug> bugs = new ArrayList<Bug>();

        for (Integer id : ids) {
            try {
                final Bug bug = helper.getBZHelper().getBug(id);
                if (bug == null)
                    continue;

                for (String target : bug.getTargetRelease()) {
                    if (target.equals(issueFixVersion)) {
                        bugs.add(bug);
                        break;
                    }
                }

            } catch (Exception ignore) {
                System.err.printf("Cannot get a bug related to the pull request %d: %s.\n", pull.getNumber(), ignore);
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

}
