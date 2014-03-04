/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.RedhatPullRequest;
import org.jboss.pull.shared.connectors.github.GithubHelper;
import org.jboss.pull.shared.evaluators.PullEvaluatorFacade;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * A shared functionality regarding mergeable PRs, Github and Bugzilla.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 * @author wangchao
 */
public class PullHelper {
    public static final Pattern BUILD_OUTCOME = Pattern.compile(
            "outcome was (\\*\\*)?+(SUCCESS|FAILURE|ABORTED)(\\*\\*)?+ using a merge of ([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern PENDING = Pattern.compile(".*Build.*merging.*has\\W+been\\W+triggered.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern RUNNING = Pattern.compile(".*Build.*merging.*has\\W+been\\W+started.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern FINISHED = Pattern.compile(".*Build.*merging.*has\\W+been\\W+finished.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern MERGE = Pattern.compile(".*(re)?merge\\W+this\\W+please.*", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);
    public static final Pattern FORCE_MERGE = Pattern.compile(".*force\\W+merge\\W+this.*", Pattern.CASE_INSENSITIVE
            | Pattern.DOTALL);

    // private final Properties props;

    private final PullEvaluatorFacade evaluatorFacade;

    private final UserList adminList;

    // ------- Specific Helpers
    private final GithubHelper ghHelper;
    private final BZHelper bzHelper;

    private final Properties props;

    public Properties getProperties() {
        return props;
    }

    public PullHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {
            ghHelper = new GithubHelper(configurationFileProperty, configurationFileDefault);
            bzHelper = new BZHelper(configurationFileProperty, configurationFileDefault);

            props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

            // initialize evaluators
            evaluatorFacade = new PullEvaluatorFacade(this, props);

            adminList = UserList.loadUserList(Util.require(props, "admin.list.file"));

        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }
    }

    public PullEvaluatorFacade getEvaluatorFacade() {
        return evaluatorFacade;
    }

    public List<RedhatPullRequest> getOpenPullRequests() {
        List<PullRequest> pullRequests = ghHelper.getPullRequests("open");

        List<RedhatPullRequest> redhatPullRequests = new ArrayList<RedhatPullRequest>();

        for (PullRequest pullRequest : pullRequests) {
            redhatPullRequests.add(new RedhatPullRequest(pullRequest, bzHelper, ghHelper));
        }

        return redhatPullRequests;
    }

    public RedhatPullRequest getPullRequest(String organization, String repository, int id){
        PullRequest pullRequest = ghHelper.getPullRequest(organization, repository, id);
        return new RedhatPullRequest(pullRequest, bzHelper, ghHelper);
    }

    public List<Milestone> getGithubMilestones() {
        return ghHelper.getMilestones();
    }

    public Milestone createMilestone(String title) {
        return ghHelper.createMilestone(title);
    }

    /**
     * Checks the state of the given pull request from the pull-processor perspective.
     *
     * @param pullRequest the pull request
     * @return relevant state
     */
    public ProcessorPullState checkPullRequestState(final RedhatPullRequest pullRequest) {
        ProcessorPullState result = ProcessorPullState.NEW;

        final List<Comment> comments = pullRequest.getGithubComments();
        for (Comment comment : comments) {
            if (ghHelper.getGithubLogin().equals(comment.getUser().getLogin())) {
                if (PENDING.matcher(comment.getBody()).matches()) {
                    result = ProcessorPullState.PENDING;
                    continue;
                }

                if (RUNNING.matcher(comment.getBody()).matches()) {
                    result = ProcessorPullState.RUNNING;
                    continue;
                }

                if (FINISHED.matcher(comment.getBody()).matches()) {
                    result = ProcessorPullState.FINISHED;
                    continue;
                }
            }

            if (MERGE.matcher(comment.getBody()).matches()) {
                result = ProcessorPullState.MERGEABLE;
                continue;
            }
        }

        if (result == ProcessorPullState.MERGEABLE || result == ProcessorPullState.NEW) {
            // check other conditions, i.e. upstream pull request and bugzilla and jira...
            final PullEvaluator.Result mergeable = evaluatorFacade.isMergeable(pullRequest);
            if (!mergeable.isMergeable()) {
                result = ProcessorPullState.INCOMPLETE;
            }

            if (result == ProcessorPullState.INCOMPLETE && !comments.isEmpty()) {
                Comment lastComment = comments.get(comments.size() - 1);
                if (FORCE_MERGE.matcher(lastComment.getBody()).matches() && isAdminUser(lastComment.getUser().getLogin()))
                    result = ProcessorPullState.MERGEABLE;
            }
        }

        return result;
    }

    public boolean isAdminUser(final String username) {
        return adminList.has(username);
    }

}
