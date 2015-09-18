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
import java.util.Properties;

import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;

public interface IssueTrackerService {

    /**
     * Initiate this <code>IssueTrackerService</code> using the supplied properties object.
     *
     * @param properties A properties object containing all configuration information required by
     *                   the IssueTrackerService.
     */
    void init(Properties properties);

    /**
     * Sets the base url of this <code>IssueTrackerService</code>.
     *
     * @param url the base url of the <code>IssueTrackerService</code>
     * @return <code>true</code> if this URL exists and has not previously been set.
     */
    boolean setBaseUrl(URL url);

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
     * Update an <code>Issue</code> at the remote issue tracker service.
     *
     * @param issue the issue to be updated at the <code>IssueTrackerService</code>
     * @throws NotFoundException if the provided <code>Issue</code> cannot be found at the IssueTracker.
     */
    void updateIssue(Issue issue) throws NotFoundException;

}
