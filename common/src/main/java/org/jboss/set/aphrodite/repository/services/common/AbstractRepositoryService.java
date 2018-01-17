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
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author Ryan Emerson
 */
public abstract class AbstractRepositoryService {

    protected final RepositoryType REPOSITORY_TYPE;
    protected RepositoryConfig config;
    protected URL baseUrl;

    protected abstract Log getLog();

    public AbstractRepositoryService(RepositoryType REPOSITORY_TYPE) {
        this.REPOSITORY_TYPE = REPOSITORY_TYPE;
    }

    public boolean init(AphroditeConfig aphroditeConfig) {
        Iterator<RepositoryConfig> i = aphroditeConfig.getRepositoryConfigs().iterator();
        while (i.hasNext()) {
            RepositoryConfig config = i.next();
            if (config.getType() == REPOSITORY_TYPE) {
                // i.remove(); // Remove so that this service cannot be instantiated twice
                // Don't remove anymore, GitHubRepositoryService is initialized from Aphrodite instance,
                // GithubPullRequestHomeService is initialize from container.
                return init(config);
            }
        }
        return false;
    }

    public boolean init(RepositoryConfig config) {
        this.config = config;
        String url = config.getUrl();
        if (!url.endsWith("/"))
            url = url + "/";

        try {
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            String errorMsg = "Invalid Repository url. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(getLog(), errorMsg, e);
            return false;
        }
        return true;
    }

    public boolean urlExists(URL url) {
        Objects.requireNonNull(url);
        return url.getHost().equals(baseUrl.getHost());
    }

    protected void checkHost(URL url) throws NotFoundException {
        if (!urlExists(url))
            throw new NotFoundException("The requested Repository cannot be found as it is not " +
                    "hosted on this server.");
    }
}
