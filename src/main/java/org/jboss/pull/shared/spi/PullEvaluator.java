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
package org.jboss.pull.shared.spi;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.connectors.bugzilla.Issue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * A pull request evaluator service interface.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public interface PullEvaluator {

    /**
     * Initializes the evaluator.
     * @param helper {@code PullHelper} instance
     * @param configuration configuration properties
     * @param version issueFixVersion name related to this evaluator in the configuration properties
     */
    void init(final PullHelper helper, final Properties configuration, final String version);

    /**
     * Returns the github branch this evaluator is dedicated to.
     * @return the target branch this evaluator is dedicated to.
     */
    String getTargetBranch();

    /**
     * Evaluates if the given pull request is mergeable according to
     * the rules of the relevant EAP issueFixVersion.
     * @param pull a pull request to be evaluated
     * @return {@code Result} result of the evaluation
     */
    Result isMergeable(final PullRequest pull);

    /**
     * Returns the issue(-s) related to the given pull request.
     * It can either be a {@code JiraIssue} if the pull request is tracked in Jira
     * or a {@code Bug} if it is tracked by Bugzilla or a list of both.
     * @param pull a pull request
     * @return the issue(-s) related to the pull request.
     */
    List<? extends Issue> getIssue(final PullRequest pull);

    /**
     * Returns the upstream pull request(-s) related to the given pull request.
     * @param pull a pull request
     * @return the upstream pull request(-s) related to the pull request.
     */
    List<PullRequest> getUpstreamPullRequest(final PullRequest pull);

    /**
     * Marks the issue related to the given pull request merged.
     * Typically in Bugzilla to state MODIFIED, in JIRA to state Resolved.
     * @param pull a pull request
     * @return true if the issue has been updated, false otherwise
     */
    boolean updateIssueAsMerged(final PullRequest pull);


    /**
     * Result of the evaluation process of a pull request.
     * It holds a simple boolean whether the pull request can be merged
     * and a list of descriptions why it can/can't be done so.
     */
    public class Result {
        private boolean mergeable;
        private List<String> description;

        public Result() {
            this.description = new ArrayList<String>();
        }

        public Result(final boolean mergeable) {
            this.mergeable = mergeable;
            this.description = new ArrayList<String>();
        }

        public Result(final boolean mergeable, final String... description) {
            this.mergeable = mergeable;
            this.description = new ArrayList<String>(Arrays.asList(description));
        }

        public boolean isMergeable() {
            return mergeable;
        }

        public void setMergeable(final boolean mergeable) {
            this.mergeable = mergeable;
        }

        public List<String> getDescription() {
            return description;
        }

        public void addDescription(final String... description) {
            this.description.addAll(Arrays.asList(description));
        }

        public void addDescription(final List<String> description) {
            this.description.addAll(description);
        }

        /**
         * Logical conjunction with another {@code Result} instance.
         * @param other
         * @return
         */
        public Result and(final Result other) {
            setMergeable(isMergeable() && other.isMergeable());
            addDescription(other.getDescription());
            return this;
        }
    }
}
