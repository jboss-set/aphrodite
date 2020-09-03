/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.domain.spi;

import java.util.List;

import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;

/**
 * Pull Request Home Service SPI. Methods are detached from deprecated pull request specific methods from RepositoryService
 * GitHubRepositoryService needs to implements this.
 *
 * @author wangc
 *
 */
public interface PullRequestHome {

    /**
     * Find the referenced pull requests to the given pull request in this PullRequestHome.
     * In order to locate all the PRs in all the repos use the <em>aphrodite</em> method.
     *
     * @param pullRequest the <code>PullRequest</code> on which referenced pull requests are being searched
     * @return list of referenced pull requests.
     * @see org.jboss.set.aphrodite.Aphrodite#findReferencedPullRequests(org.jboss.set.aphrodite.domain.PullRequest)
     */
    List<PullRequest> findReferencedPullRequests(PullRequest pullRequest);

    /**
     * Add a comment to the specified <code>PullRequest</code> object, and propagate the changes to the remote repository.
     *
     * @param pullRequest the <code>PullRequest</code> on which the comment will be made.
     * @param comment the new <code>Comment</code>.
     * @return <tt>true</tt> if this comment is added as a result of the call
     */
    boolean addComment(PullRequest pullRequest, String comment);

    /**
     * Retrieve all labels associated with the provided <code>PullRequest</code> object.
     *
     * @param pullRequest the <code>PullRequest<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no pull request can be found.
     */
    List<Label> getLabels(PullRequest pullRequest);

    /**
     * Set the labels for the <code>PullRequest</code> object.
     *
     * @param pullRequest the <code>PullRequest</code> object whose will be set.
     * @param labels the <code>Label</code> apply to the <code>PullRequest</code>
     * @return <tt>true</tt> if this label is set as a result of the call
     */
    boolean setLabels(PullRequest pullRequest, List<Label> labels);

    /**
     * Attach a label to the specified pull request. Note the label must already exist at remote repository. If the specified
     * label is already associated with the provided pull request then no further action is taken.
     *
     * @param pullRequest the <code>PullRequest</code> to which the label will be applied.
     * @param labelName the name of the label to be applied.
     * @return <tt>true</tt> if this label is added as a result of the call
     */
    boolean addLabel(PullRequest pullRequest, Label label);

    /**
     * Remove a label from the provided <code>PullRequest</code> object.
     *
     * @param pullRequest the <code>PullRequest</code> whose label will be removed.
     * @param name the <code>Label</code> name will be removed.
     * @return <tt>true</tt> if a label was removed as a result of this call
     */
    boolean removeLabel(PullRequest pullRequest, Label label);

    /**
     * Retrieve the current CI status of the latest commit associated with a given pull request.
     *
     * @param pullRequest the <code>PullRequest</code> object whose status is to be queried
     * @return the CI status of the latest commit associated with the given pull request
     */
    CommitStatus getCommitStatus(PullRequest pullRequest);

    /**
     * Approve pull request.
     *
     * @param pullRequest
     */
    void approveOnPullRequest(PullRequest pullRequest);

    /**
     * Request changes on pull request with comment.
     *
     * @param pullRequest
     * @param body
     */
    void requestChangesOnPullRequest(PullRequest pullRequest, String body);
}
