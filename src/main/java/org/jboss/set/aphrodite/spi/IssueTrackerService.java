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
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;

import java.net.URL;
import java.util.List;

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
     * Retrieve all Issues associated with the provided patch object.
     *
     * @param patch the <code>Patch</code> object whoms associated Issues should be returned.
     * @return a list of all <code>Issue</code> objects, or an empty list if no issues can be found.
     */
    List<Issue> getIssuesAssociatedWith(Patch patch);

    /**
     * Retrieve an issue object associated with the given <code>URL</code>.
     *
     * @param url the <code>URL</code> of the issue to be retrieved.
     * @return the <code>Issue</code> associated with the provided <code>URK</code>.
     * @throws NotFoundException if the provided <code>URL</code> is not associated with an issue.
     */
    Issue getIssue(URL url) throws NotFoundException;

    /**
     * Return all issues which match the passed <code>SearchCriteria</code>.
     *
     * @param searchCriteria all set fields will be search for.
     * @return a list of all <code>Issue</code> objects which match the specified searchCriteria,
     *         or an empty list if no issues match the searched criteria.
     */
    List<Issue> searchIssues(SearchCriteria searchCriteria);

    /**
     * Update an <code>Issue</code> at the remote issue tracker service.
     *
     * @param issue the issue to be updated at the <code>IssueTrackerService</code>
     * @return true if the issue was successfully updated, false otherwise.
     * @throws NotFoundException if the provided <code>Issue</code> cannot be found at the IssueTracker.
     */
    boolean updateIssue(Issue issue) throws NotFoundException;

}
