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
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;

import java.net.URL;
import java.util.List;

public interface RepositoryService {

    /**
     * Initiate this <code>RepositoryService</code> using the supplied properties object.
     *
     * @param config a <code>AphroditeConfig</code> object containing all configuration information
     *               required by a RepositoryService.
     */
    void init(AphroditeConfig config);

    /**
     * Sets the base url of this <code>RepositoryService</code>.
     *
     * @param url the base url of the <code>RepositoryService</code>
     * @return <code>true</code> if this URL exists and has not previously been set.
     */
    boolean setBaseUrl(URL url);

    /**
     * Get the repository located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the repository to be retrieved.
     * @return the <code>Repository</code> object.
     * @throws NotFoundException if a <code>Repository</code> cannot be found at the provided base url.
     */
    Repository getRepository(URL url) throws NotFoundException;

    /**
     * Retrieve all Patches associated with the provided <code>Issue</code> object
     *
     * @param issue the <code>Issue</code> object whose associated Patches should be returned.
     * @return a list of all <code>Patch</code> objects, or an empty list if no patches can be found.
     */
    List<Patch> getPatchesAssociatedWith(Issue issue);

    /**
     * Retrieve all Patches associated with the provided <code>Repository</code> object, which have a
     * status that matches the provided <code>PatchStatus</code> object.
     *
     * @param repository the <code>Repository</code> object whose associated Patches should be returned.
     * @param status the <code>PatchStatus</code> which the returned <code>Patch</code> objects must have.
     * @return a list of all matching <code>Patch</code> objects, or an empty list if no patches can be found.
     */
    List<Patch> getPatchesByStatus(Repository repository, PatchStatus status);

    /**
     * Add a <code>Comment</code> to the specified <code>Patch</code> object, and propagate the changes
     * to the remote repository.
     *
     * @param patch the <code>Patch</code> on which the comment will be made.
     * @param comment the new <code>Comment</code>.
     * @throws NotFoundException if the <code>Patch</code> cannot be found at the remote repository.
     */
    void addComment(Patch patch, String comment) throws NotFoundException;
}
