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
import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.github.GithubHelper;
import org.jboss.pull.shared.evaluators.PullEvaluatorFacade;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A shared functionality regarding mergeable PRs, Github and Bugzilla.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 * @author wangchao
 */
public class PullHelper {
    private static final Pattern BUILD_OUTCOME = Pattern.compile(
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

//    private final Properties props;

    private final PullEvaluatorFacade evaluatorFacade;

    private final UserList adminList;

    // ------- Specific Helpers
    private final GithubHelper ghHelper;
    public GithubHelper getGHHelper(){
        return ghHelper;
    }
    private final BZHelper bzHelper;
    public BZHelper getBZHelper(){
        return bzHelper;
    }

    private final Properties props;
    public Properties getProperties(){
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

    /**
     * Checks the state of the given pull request from the pull-processor perspective.
     *
     * @param pullRequest the pull request
     * @return relevant state
     */
    public ProcessorPullState checkPullRequestState(final PullRequest pullRequest) {
        ProcessorPullState result = ProcessorPullState.NEW;

        try {
            final List<Comment> comments = ghHelper.getComments(pullRequest);
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

        } catch (IOException e) {
            System.err.printf("Cannot read comments of PR#%d due to %s\n", pullRequest.getNumber(), e);
            result = ProcessorPullState.ERROR;
        }

        return result;
    }

    public boolean isAdminUser(final String username) {
        return adminList.has(username);
    }

    public boolean isMerged(final PullRequest pullRequest) {
        if (pullRequest == null) {
            return false;
        }

        if (!pullRequest.getState().equals("closed")) {
            return false;
        }

        try {
            if (ghHelper.isMerged(pullRequest)) {
                return true;
            }
        } catch (IOException ignore) {
            System.err.printf("Cannot get Merged information of the pull request %d: %s.\n", pullRequest.getNumber(), ignore);
            ignore.printStackTrace(System.err);
        }

        try {
            final List<Comment> comments = ghHelper.getComments(pullRequest);
            for (Comment comment : comments) {
                if (comment.getBody().toLowerCase().indexOf("merged") != -1) {
                    return true;
                }
            }
        } catch (IOException ignore) {
            System.err.printf("Cannot get comments of the pull request %d: %s.\n", pullRequest.getNumber(), ignore);
            ignore.printStackTrace(System.err);
        }

        return false;
    }

    public BuildResult checkBuildResult(PullRequest pullRequest) {
        BuildResult buildResult = BuildResult.UNKNOWN;
        List<Comment> comments;
        try {
            comments = ghHelper.getComments(pullRequest);
        } catch (IOException e) {
            System.err.println("Error to get comments for pull request : " + pullRequest.getNumber());
            e.printStackTrace(System.err);
            return buildResult;
        }
        for (Comment comment : comments) {
            Matcher matcher = BUILD_OUTCOME.matcher(comment.getBody());
            while (matcher.find()) {
                buildResult = BuildResult.valueOf(matcher.group(2));
            }
        }
        return buildResult;
    }

}
