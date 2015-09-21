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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.pull.shared.Util;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ServiceLoader;

public class Aphrodite {

    public static final String FILE_LOCATION = "aphrodite.file";

    private static final Log LOG = LogFactory.getLog(Aphrodite.class);
    private static Aphrodite instance;

    /**
     * Get an instance of the Aphrodite service. If the service has not yet been initialised, then
     * a new service is created.
     *
     * This service will use the YAML configuration file specified in the {@value FILE_LOCATION}
     * environment variable.
     *
     * @return instance the singleton instance of the Aphrodite service.
     * @throws AphroditeException if the specified configuration file cannot be opened.
     */
    public static synchronized Aphrodite instance() throws AphroditeException {
        if (instance == null) {
            instance = new Aphrodite();
        }
        return instance;
    }

    /**
     * Get an instance of the Aphrodite service. If the service has not yet been initialised, then
     * a new service is created using the provided config. If the service has already been initialised
     * then an <code>IllegalStateException</code> is thrown.
     *
     * @param config an <code>AphroditeConfig</code> object containing all configuration data.
     * @return instance the singleton instance of the Aphrodite service.
     * @throws AphroditeException
     * @throws IllegalStateException if an <code>Aphrodite</code> service has already been initialised.
     */
    public static synchronized Aphrodite instance(AphroditeConfig config) throws AphroditeException {
        if (instance != null)
            throw new IllegalStateException("Cannot create a new instance of " +
                    Aphrodite.class.getName() + " as it is a singleton which has already been initialised.");

        instance = new Aphrodite(config);
        return instance();
    }

    private AphroditeConfig config;

    private Aphrodite() throws AphroditeException {
        String propFileLocation = System.getenv(FILE_LOCATION);
        if (propFileLocation == null)
            throw new IllegalArgumentException("Environment variable '" + FILE_LOCATION + "' must be set");

        try (InputStream is = new FileInputStream(System.getenv(FILE_LOCATION))) {
            init(new Yaml().loadAs(is, AphroditeConfig.class));
        } catch (IOException e) {
            Util.logException(LOG, "Unable to load file: " + propFileLocation, e);
            throw new AphroditeException(e);
        }
    }

    private Aphrodite(AphroditeConfig config) throws AphroditeException {
        init(config);
    }

    private void init(AphroditeConfig config) {
        this.config = config;

        // Create new config object, as the object passed to init() will have its state changed.
        AphroditeConfig mutableConfig = new AphroditeConfig(config);
        ServiceLoader.load(IssueTrackerService.class).forEach(issueTracker -> issueTracker.init(mutableConfig));
        ServiceLoader.load(RepositoryService.class).forEach(repositoryService -> repositoryService.init(mutableConfig));
    }

    public static void main(String[] args) throws Exception {
        Aphrodite.instance();
    }
}
