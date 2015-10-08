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

package org.jboss.set.aphrodite.repository.services.github;


import org.eclipse.egit.github.core.RepositoryBranch;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Repository;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ryan Emerson
 */
class GitHubWrapper {

    Repository toAphroditeRepository(URL url, List<RepositoryBranch> branches) {
        Repository repo = new Repository(url);
        List<Codebase> branchNames = branches.stream()
                .map(branch -> new Codebase(branch.getName()))
                .collect(Collectors.toList());
        repo.getCodebases().addAll(branchNames);
        return repo;
    }
}
