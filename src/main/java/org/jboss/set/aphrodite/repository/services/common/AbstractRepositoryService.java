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

package org.jboss.set.aphrodite.repository.services.common;

import org.apache.commons.logging.Log;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ryan Emerson
 */
public abstract class AbstractRepositoryService implements RepositoryService {

    protected final RepositoryType REPOSITORY_TYPE;
    protected RepositoryConfig config;
    protected URL baseUrl;

    protected abstract Log getLog();

    public AbstractRepositoryService(RepositoryType REPOSITORY_TYPE) {
        this.REPOSITORY_TYPE = REPOSITORY_TYPE;
    }

    @Override
    public boolean init(AphroditeConfig aphroditeConfig) {
        Iterator<RepositoryConfig> i = aphroditeConfig.getRepositoryConfigs().iterator();
        while (i.hasNext()) {
            RepositoryConfig config = i.next();
            if (config.getType() == REPOSITORY_TYPE) {
                i.remove(); // Remove so that this service cannot be instantiated twice
                return init(config);
            }
        }
        return false;
    }

    @Override
    public boolean init(RepositoryConfig config) {
        this.config = config;
        String url = config.getUrl();
        if (!url.endsWith("/"))
            url = url + "/";

        try {
            baseUrl = new URL(config.getUrl());
        } catch (MalformedURLException e) {
            String errorMsg = "Invalid Repository url. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(getLog(), errorMsg, e);
            return false;
        }
        return true;
    }

    @Override
    public Repository getRepository(URL url) throws NotFoundException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Patch getPatch(URL url) throws NotFoundException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public List<Patch> getPatchesAssociatedWith(Issue issue) throws NotFoundException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public List<Patch> getPatchesByStatus(Repository repository, PatchStatus status) throws NotFoundException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void addCommentToPatch(Patch patch, String comment) throws NotFoundException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    protected void checkHost(URL url) throws NotFoundException {
        if (!url.getHost().equals(baseUrl.getHost()))
            throw new NotFoundException("The requested Repository cannot be found as it is not " +
                    "hosted on this server.");
    }
}
