/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.spi;

import java.net.URL;
import java.util.List;

import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;

public interface RepositoryService {

    /**
     * Initiate this <code>RepositoryService</code> using the supplied <code>AphroditeConfig</code>.
     * The first <code>RepositoryConfig</code> object found in <code>AphroditeConfig</code> object,
     * is used to initiate the service and is subsequently removed from the config to prevent the same
     * service being initiated twice.
     *
     * @param config a <code>AphroditeConfig</code> object containing at least one
     *               <code>RepositoryConfig</code> object.
     * @return <code>true</code> if the service was initialised without errors, <code>false</code> otherwise.
     * @throws IllegalArgumentException if no <code>RepositoryConfig</code> objects are present.
     */
    boolean init(AphroditeConfig config);

    /**
     * Initiate this <code>RepositoryService</code> using the supplied <code>RepositoryConfig</code>.
     *
     * @param config a <code>RepositoryConfig</code> object containing all configuration information.
     * @return <code>true</code> if the service was initialised without errors, <code>false</code> otherwise.
     */
    boolean init(RepositoryConfig config);

    /**
     * Checks whether the provided <code>URL</code> is on the same host as this service.
     * @param url the <code>URL</code> to check.
     * @return true if the provided <code>URL</code> has the same host as this service, otherwise false.
     * @throws NullPointerException if the provided <code>URL</code> is null.
     */
    boolean urlExists(URL url);

    /**
     * Checks whether the provided <code>URL</code> is accessable.
     * @param url the <code>URL</code> to check.
     * @return true if the provided <code>URL</code> is accessable, otherwise false.
     */
    boolean repositoryAccessable(URL url);

    /**
     * Get the repository located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the repository to be retrieved.
     * @return the <code>Repository</code> object.
     * @throws NotFoundException if a <code>Repository</code> cannot be found at the provided base url.
     */
    Repository getRepository(URL url) throws NotFoundException;

    /**
     * Get the <code>PullRequest</code> located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the pull request to be retrieved.
     * @return the <code>PullRequest</code> object.
     * @throws NotFoundException if a <code>PullRequest</code> cannot be found at the provided base url.
     */
    PullRequest getPullRequest(URL url) throws NotFoundException;

    /**
     * Retrieve all pull requests associated with the provided <code>Issue</code> object
     *
     * @param issue the <code>Issue</code> object whose associated pull requests should be returned.
     * @return a list of all <code>PullRequest</code> objects, or an empty list if no pull request can be found.
     * @throws NotFoundException if an exception is thrown when searching the RepositoryService.
     */
    @Deprecated
    List<PullRequest> getPullRequestsAssociatedWith(Issue issue) throws NotFoundException;

    /**
     * Retrieve all pull requests associated with the provided <code>Repository</code> object, which have a
     * state that matches the provided <code>PullRequestState</code> object.
     *
     * @param repository the <code>Repository</code> object whose associated pull requests should be returned.
     * @param state the <code>PullRequestsState</code> which the returned <code>PullRequest</code> objects must have.
     * @return a list of all matching <code>PullRequest</code> objects, or an empty list if no pull request can be found.
     * @throws NotFoundException if the provided <code>Repository</code> cannot be found at the RepositoryService.
     */
    List<PullRequest> getPullRequestsByState(Repository repository, PullRequestState state) throws NotFoundException;

    /**
     * Retrieve all labels associated with the provided <code>PullRequest</code> in <code>Repository</code> object.
     * @param repository the <code>Repository<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no label can be found.
     * @throws NotFoundException if the provided <code>Repository</code> url not consistent with the baseURL.
     */
    List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException;

    /**
     * Retrieve all labels associated with the provided <code>PullRequest</code> object.
     * @param pullRequest the <code>PullRequest<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no pull request can be found.
     * @throws NotFoundException if the provided <code>PullRequest</code> url not consistent with the baseURL.
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#getLabels()} instead.
     */
    @Deprecated
    List<Label> getLabelsFromPullRequest(PullRequest pullRequest) throws NotFoundException;

    /**
     * Discover if the user logged into this <code>RepositoryService</code> has the correct permissions to apply/remove
     * labels to pull request in the provided <code>Repository</code>
     * @param repository the <code>Repository</code> whose permissions are to be checked
     * @return true if the user has permission, otherwise false.
     * @throws NotFoundException if the specified <code>Repository</code> cannot be found.
     */
    boolean hasModifiableLabels(Repository repository) throws NotFoundException;

    /**
     * Set the labels for the provided <code>PullRequest</code> object.
     * @param pullRequest the <code>PullRequest</code> object whose will be set.
     * @param labels the <code>Label</code> apply to the <code>PullRequest</code>
     * @throws NotFoundException if the <code>Label</code> can not be found in the provided <code>PullRequest</code>
     * @throws AphroditeException if add the <code>Label<code> is not consistent with get labels
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#setLabels()} instead.
     */
    @Deprecated
    void setLabelsToPullRequest(PullRequest pullRequest, List<Label> labels) throws NotFoundException, AphroditeException ;

    /**
     *Delete a label from the provided <code>PullRequest</code> object.
     * @param pullRequest the <code>PullRequest</code> whose label will be removed.
     * @param name the <code>Label</code> name will be removed.
     * @throws NotFoundException if the <code>Label</code> name can not be found in the provided <code>PullRequest</code>
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#removeLabel()} instead.
     */
    @Deprecated
    void removeLabelFromPullRequest(PullRequest pullRequest, String name) throws NotFoundException;

    /**
     * Add a <code>Comment</code> to the specified <code>PullRequest</code> object, and propagate the changes
     * to the remote repository.
     *
     * @param pullRequest the <code>PullRequest</code> on which the comment will be made.
     * @param comment the new <code>Comment</code>.
     * @throws NotFoundException if the <code>PullRequest</code> cannot be found at the remote repository.
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#addComment()} instead.
     */
    @Deprecated
    void addCommentToPullRequest(PullRequest pullRequest, String comment) throws NotFoundException;

    /**
     * Attach a label to the specified pull request.  Note the label must already exist at remote repository,
     * otherwise a <code>NotFoundException</code> will be thrown. If the specified label is already
     * associated with the provided pull request then no further action is taken.
     *
     * @param pullRequest the <code>PullRequest</code> to which the label will be applied.
     * @param labelName the name of the label to be applied.
     * @throws NotFoundException if the specified labelName has not been defined at the remote repository.
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#addLabel()} instead.
     */
    @Deprecated
    void addLabelToPullRequest(PullRequest pullRequest, String labelName) throws NotFoundException;

    /**
     * Find all the pull requests related to the given pull request.
     *
     * @param pullRequest the <code>PullRequest</code> on which pull requests related are being searched
     * @return list of pull requests related.
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#findReferencedPullRequests()} instead.
     */
    @Deprecated
    List<PullRequest> findPullRequestsRelatedTo(PullRequest pullRequest);

    /**
     * Retrieve the current CI status of the latest commit associated with a given pull request.
     *
     * @param pullRequest the <code>PullRequest</code> object whose status is to be queried
     * @return the CI status of the latest commit associated with the given pull request
     * @throws NotFoundException if no commit status can be found for the provided pull request
     * @deprecated Use {@link org.jboss.set.aphrodite.domain.spi.PullRequestHome#getCommitStatus()} instead.
     */
    @Deprecated
    CommitStatus getCommitStatusFromPullRequest(PullRequest pullRequest) throws NotFoundException;

    /**
     * allows to destroy and deallocate resources
     */
    default void destroy() {
    }

    RateLimit getRateLimit() throws NotFoundException;

    /** Get Repository type
     * @return RepositoryType
     */
    RepositoryType getRepositoryType();

    /**
     * Return the PullRequestHome service for this repository.
     *
     * @return Returns The PR home service for this repository.
     */
    PullRequestHome getPullRequestHome();
}
