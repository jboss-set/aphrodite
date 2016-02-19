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
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;

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
     * Get the repository located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the repository to be retrieved.
     * @return the <code>Repository</code> object.
     * @throws NotFoundException if a <code>Repository</code> cannot be found at the provided base url.
     */
    Repository getRepository(URL url) throws NotFoundException;

    /**
     * Get the <code>Patch</code> located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the patch to be retrieved.
     * @return the <code>Patch</code> object.
     * @throws NotFoundException if a <code>Patch</code> cannot be found at the provided base url.
     */
    Patch getPatch(URL url) throws NotFoundException;

    /**
     * Retrieve all Patches associated with the provided <code>Issue</code> object
     *
     * @param issue the <code>Issue</code> object whose associated Patches should be returned.
     * @return a list of all <code>Patch</code> objects, or an empty list if no patches can be found.
     * @throws NotFoundException if the provided <code>Repository</code> cannot be found at the RepositoryService.
     */
    List<Patch> getPatchesAssociatedWith(Issue issue) throws NotFoundException;

    /**
     * Retrieve all Patches associated with the provided <code>Repository</code> object, which have a
     * status that matches the provided <code>PatchStatus</code> object.
     *
     * @param repository the <code>Repository</code> object whose associated Patches should be returned.
     * @param status the <code>PatchStatus</code> which the returned <code>Patch</code> objects must have.
     * @return a list of all matching <code>Patch</code> objects, or an empty list if no patches can be found.
     * @throws NotFoundException if the provided <code>Repository</code> cannot be found at the RepositoryService.
     */
    List<Patch> getPatchesByStatus(Repository repository, PatchStatus status) throws NotFoundException;

    /**
     * Add a <code>Comment</code> to the specified <code>Patch</code> object, and propagate the changes
     * to the remote repository.
     *
     * @param patch the <code>Patch</code> on which the comment will be made.
     * @param comment the new <code>Comment</code>.
     * @throws NotFoundException if the <code>Patch</code> cannot be found at the remote repository.
     */
    void addCommentToPatch(Patch patch, String comment) throws NotFoundException;

    /**
     * Attach a label to the specified patch.  Note the label must already exist at remote repository,
     * otherwise a <code>NotFoundException</code> will be thrown. If the specified label is already
     * associated with the provided patch then no further action is taken.
     *
     * @param patch the <code>Patch</code> to which the label will be applied.
     * @param labelName the name of the label to be applied.
     * @throws NotFoundException if the specified labelName has not been defined at the remote repository.
     */
    void addLabelToPatch(Patch patch, String labelName) throws NotFoundException;
    
    /**
     * Find all the patches related to the given patch. 
     * 
     * @param patch the <code>Patch</code> on which patches related are being searched
     * @return list of patches related. 
     * @throws NotFoundException if the <code>Patch</code> cannot be found at the remote repository.
     */
    List<Patch> findPatchesRelatedTo(Patch patch) throws NotFoundException;
}
