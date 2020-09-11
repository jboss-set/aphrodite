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

import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.issue.trackers.common.IssueCreationDetails;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IssueTrackerService {

    /**
     * Initiate this <code>IssueTrackerService</code> using the supplied <code>AphroditeConfig</code>.
     * The first <code>IssueTrackerConfig</code> object found in the <code>AphroditeConfig</code> object,
     * is used to initiate the service and is subsequently removed from the config to prevent the same
     * service being initiated twice.
     *
     * @param config a <code>AphroditeConfig</code> object containing at least one
     *               <code>IssueTrackerConfig</code> object.
     * @return <code>true</code> if the service was initialised without errors, <code>false</code> otherwise.
     * @throws IllegalArgumentException if no <code>IssueTrackerConfig</code> objects are present.
     */
    boolean init(AphroditeConfig config);

    /**
     * Initiate this <code>IssueTrackerService</code> using the supplied <code>IssueTrackerConfig</code>.
     *
     * @param config a <code>IssueTrackerConfig</code> object containing all configuration information.
     * @return <code>true</code> if the service was initialised without errors, <code>false</code> otherwise.
     */
    boolean init(IssueTrackerConfig config);

    /**
     * Checks whether the provided <code>URL</code> is on the same host as this service.
     * @param url the <code>URL</code> to check.
     * @return true if the provided <code>URL</code> has the same host as this service, otherwise false.
     * @throws NullPointerException if the provided <code>URL</code> is null.
     */
    boolean urlExists(URL url);

    /**
     * Return string format of tracker id, this value, can be used as key/index and compared to {@link AbstractIssueTracker#convertToTrackerID()}.
     * @return
     */
    String getTrackerID();

    /**
     * Retrieve all Issues associated with the provided pullRequest object.
     * Implementations of this method assume that the urls of the related issues are present in the
     * pullRequest's description field.
     *
     * @param pullRequest the <code>PullRequest</code> object whoms associated Issues should be returned.
     * @return a list of all <code>Issue</code> objects, or an empty list if no issues can be found.
     */
    List<Issue> getIssuesAssociatedWith(PullRequest pullRequest);

    /**
     * Retrieve an issue object associated with the given <code>URL</code>.
     *
     * @param url the <code>URL</code> of the issue to be retrieved.
     * @return the <code>Issue</code> associated with the provided <code>URK</code>.
     * @throws NotFoundException if the provided <code>URL</code> is not associated with an issue.
     */
    Issue getIssue(URL url) throws NotFoundException;

    /**
     * Retrieve all issues associated with the provided URLs. This method simply logs any issue URLs
     * that cannot be retrieved from this <code>IssueTrackerServer</code>. If the provided URLs
     * collection is empty, or no issues are found, then an empty List is returned.
     *
     * @param urls a collection of issue URLs.
     * @return a list of <code>Issue</code> objects associated with the provided urls.
     */
    List<Issue> getIssues(Collection<URL> urls);

    /**
     * Return all issues which match the passed <code>SearchCriteria</code>.
     *
     * @param searchCriteria all set fields will be search for.
     * @return a list of all <code>Issue</code> objects which match the specified searchCriteria,
     *         or an empty list if no issues match the searched criteria or the searchCriteria object contains no entries.
     */
    List<Issue> searchIssues(SearchCriteria searchCriteria);

    /**
     * Return all issues which match the provided filter.
     *
     * @param filterUrl the url of the issue tracker filtered to be applied.
     * @return a list of all <code>Issue</code> objects which are returned by the provided filter.
     * @throws NotFoundException if the filterURL is not associated with any filters.
     */
    List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException;

    /**
     * Update an <code>Issue</code> at the remote issue tracker service.
     *
     * Note, this does not update issue comments or an issues description.
     * To add a new comment, use {@link #addCommentToIssue(Issue, Comment)}
     *
     * @param issue the issue to be updated at the <code>IssueTrackerService</code>
     * @return true if the issue was successfully updated, false otherwise.
     * @throws NotFoundException if the provided <code>Issue</code> cannot be found at the IssueTracker.
     * @throws AphroditeException if the user credentials supplied for this issue track do not have
     *                               permission to update this issue, or a field within this issue.
     */
    boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException;

    /**
     * Adds a new comment to the specified issue.
     *
     * @param issue the issue to add a new comment to.
     * @param comment the comment to be added to the issue.
     * @throws NotFoundException if the provided <code>Issue</code> cannot be found at the IssueTracker.
     */
    void addCommentToIssue(Issue issue, Comment comment) throws NotFoundException;

    /**
     * Adds the <code>Comment</code> to the associated <code>Issue</code> object for all Issue/Comment
     * pairs in the <code>Map</code>. Null comments are ignored.
     *
     * @param commentMap the map containing all Issues that are to be updated and the associated comments.
     * @return true if all comments are successfully added to their associated Issue, otherwise false.
     */
    boolean addCommentToIssue(Map<Issue, Comment> commentMap);

    /**
     * Adds the <code>Comment</code> to all of the provided <code>Issue</code> objects.
     *
     * @param issues a collection of all issues that the comment should be added to.
     * @param comment the comment to be added to all issues.
     * @return true if the comment is successfully added to all issues.
     */
    boolean addCommentToIssue(Collection<Issue> issues, Comment comment);

    /**
     * Check if a given CP version is released.
     *
     * @param cpVersion the CP version to be tested. Jira accepts GA version format x.y.z.GA, e.g. 7.1.2.GA. Bugzilla accepts version format x.y.z, e.g. 6.4.18.
     * @return true if the given version is released, otherwise false.
     *
     */
    boolean isCPReleased(String cpVersion);

    /**
     * allows to destroy and deallocate resources
     */
    default void destroy() { }

    /**
     * Create skeletal issue.
     * @param details - implementation of issue detail proper for identified tracker
     * @return issue that was created
     * @throws NotFoundException
     * @throws MalformedURLException
     */
    Issue createIssue(final IssueCreationDetails details) throws MalformedURLException, NotFoundException;
}
