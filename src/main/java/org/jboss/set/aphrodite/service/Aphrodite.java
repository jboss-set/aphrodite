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

package org.jboss.set.aphrodite.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

// this will be the façade whenever the services are ready
public class Aphrodite {

    public static final String FILE_LOCATION = "aphrodite.file";

    private static Aphrodite instance;

    public static synchronized Aphrodite instance() throws AphroditeException {
        if (instance == null) {
            instance = new Aphrodite();
        }
        return instance;
    }

    private List<IssueTrackerService> issueTrackers;

    private List<RepositoryService> repositories;

    private Aphrodite() throws AphroditeException {
        String propFileLocation = System.getenv(FILE_LOCATION);
        if (propFileLocation == null)
            throw new IllegalArgumentException("Environment variable '" + FILE_LOCATION + "' must be set");

        try (InputStream is = new FileInputStream(System.getenv(FILE_LOCATION))) {
            issueTrackers = new ArrayList<>();
            repositories = new ArrayList<>();

            Properties properties = new Properties();
            properties.load(is);

            ServiceLoader.load(IssueTrackerService.class).forEach(issueTracker -> issueTracker.init(properties));
            ServiceLoader.load(RepositoryService.class).forEach(repositoryService -> repositoryService.init(properties));
        } catch (IOException e) {
            throw new AphroditeException(e);
        }
    }
}
