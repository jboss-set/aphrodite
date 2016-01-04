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

package org.jboss.set.aphrodite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonReader;

public class Aphrodite {

    public static final String FILE_PROPERTY = "aphrodite.config";

    private static final Log LOG = LogFactory.getLog(Aphrodite.class);
    private static Aphrodite instance;

    /**
     * Get an instance of the Aphrodite service. If the service has not yet been initialised, then
     * a new service is created.
     *
     * This service will use the JSON configuration file specified in the {@value FILE_PROPERTY}
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

    private final List<IssueTrackerService> issueTrackers = new ArrayList<>();
    private final List<RepositoryService> repositories = new ArrayList<>();

    private AphroditeConfig config;

    private Aphrodite() throws AphroditeException {
        String propFileLocation = System.getProperty(FILE_PROPERTY);
        if (propFileLocation == null)
            throw new IllegalArgumentException("Property '" + FILE_PROPERTY + "' must be set");

        try (JsonReader jr = Json.createReader(new FileInputStream(propFileLocation))) {
            init(AphroditeConfig.fromJson(jr.readObject()));
        } catch (IOException e) {
            Utils.logException(LOG, "Unable to load file: " + propFileLocation, e);
            throw new AphroditeException(e);
        }
    }

    private Aphrodite(AphroditeConfig config) throws AphroditeException {
        init(config);
    }

    private void init(AphroditeConfig config) throws AphroditeException {
        this.config = config;

        // Create new config object, as the object passed to init() will have its state changed.
        AphroditeConfig mutableConfig = new AphroditeConfig(config);

        for (IssueTrackerService is : ServiceLoader.load(IssueTrackerService.class)) {
            boolean initialised = is.init(mutableConfig);
            if (initialised)
                issueTrackers.add(is);
        }

        for (RepositoryService rs : ServiceLoader.load(RepositoryService.class)) {
            boolean initialised = rs.init(mutableConfig);
            if (initialised)
                repositories.add(rs);
        }

        if (issueTrackers.isEmpty() && repositories.isEmpty())
            throw new AphroditeException("Unable to initiatilise Aphrodite, as a valid " +
                    IssueTrackerService.class.getName() + " or " + RepositoryService.class.getName()
                    + " does not exist.");
    }

    public Issue getIssue(URL url) throws NotFoundException {
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.getIssue(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Issue not found at IssueTrackerService: " + trackerService.getClass().getName());
            }
        }
        throw new NotFoundException("No issues found which correspond to the provided url.");
    }

    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        checkIssueTrackerExists();

        List<Issue> issues = new ArrayList<>();
        issueTrackers.forEach(tracker -> issues.addAll(tracker.searchIssues(searchCriteria)));
        return issues;
    }

    public List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException {
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.searchIssuesByFilter(filterUrl);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Filter not found at IssueTrackerService: " +
                            trackerService.getClass().getName() + ":" + e);
            }
        }
        throw new NotFoundException("No filter found which correspond to the provided url.");
    }

    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.updateIssue(issue);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Issue not found at IssueTrackerService: " + trackerService.getClass().getName());
            }
        }
        throw new NotFoundException("No issues found which correspond to the provided url.");
    }

    public boolean postCommentOnIssue(Issue issue, Comment comment) {
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.addCommentToIssue(issue, comment);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Issue not found at IssueTrackerService: " + trackerService.getClass().getName());
            }
        }
        throw new IllegalStateException("No tracker service found for issue:" + issue);
    }

    public List<Issue> getIssuesAssociatedWith(Patch patch) {
        checkIssueTrackerExists();

        return issueTrackers.stream()
                .map(service -> service.getIssuesAssociatedWith(patch))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Repository getRepository(URL url) throws NotFoundException {
        checkRepositoryServiceExists();

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getRepository(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Repository not found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No repositories found which correspond to the provided url.");
    }

    public List<Patch> getPatchesAssociatedWith(Issue issue) throws NotFoundException {
        checkRepositoryServiceExists();

        List<Patch> patches = new ArrayList<>();
        for (RepositoryService repositoryService : repositories) {
            try {
                patches.addAll(repositoryService.getPatchesAssociatedWith(issue));
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        return patches;
    }

    public List<Patch> getPatchesByStatus(Repository repository, PatchStatus status) {
        checkRepositoryServiceExists();

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getPatchesByStatus(repository, status);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        return new ArrayList<>();
    }

    public Patch getPatch(URL url) throws NotFoundException {
        checkRepositoryServiceExists();

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getPatch(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No patch found which corresponds to the provided patch.");
    }

    public void addCommentToPatch(Patch patch, String comment) throws NotFoundException {
        checkRepositoryServiceExists();

        for (RepositoryService repositoryService : repositories) {
            try {
                repositoryService.addCommentToPatch(patch, comment);
                return;
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No patch found which corresponds to the provided patch.");
    }

    public void addLabelToPatch(Patch patch, String labelName) {
        checkRepositoryServiceExists();

        for (RepositoryService repositoryService : repositories) {
            try {
                repositoryService.addLabelToPatch(patch, labelName);
                return;
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
    }

    private void checkIssueTrackerExists() {
        if (issueTrackers.isEmpty())
            throw new IllegalStateException("Unable to retrieve issues as a valid " +
                    IssueTrackerService.class.getName() + " has not been created.");
    }

    private void checkRepositoryServiceExists() {
        if (repositories.isEmpty())
            throw new IllegalStateException("Unable to find any repository data as a valid " +
                    RepositoryService.class.getName() + " has not been created.");
    }
}
