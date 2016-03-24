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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;

public class Aphrodite implements AutoCloseable {

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
     * a new service is created using config. If the service has already been initialised
     * then an <code>IllegalStateException</code> is thrown if a different <code>AphroditeConfig</code> object is passed.
     *
     * @param config an <code>AphroditeConfig</code> object containing all configuration data.
     * @return instance the singleton instance of the Aphrodite service.
     * @throws AphroditeException
     * @throws IllegalStateException if an <code>Aphrodite</code> service has already been initialised.
     */
    public static synchronized Aphrodite instance(AphroditeConfig config) throws AphroditeException {
        if (instance != null) {
            if (instance.config.equals(config))
                return instance;
            throw new IllegalStateException("Cannot create a new instance of " +
                    Aphrodite.class.getName() + " as it is a singleton which has already been initialised.");
        }

        instance = new Aphrodite(config);
        return instance();
    }

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        issueTrackers.forEach(IssueTrackerService::destroy);
        issueTrackers.clear();
        repositories.forEach(RepositoryService::destroy);
        repositories.clear();
    }

    private final List<IssueTrackerService> issueTrackers = new ArrayList<>();
    private final List<RepositoryService> repositories = new ArrayList<>();
    private ExecutorService executorService;

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

        executorService = config.getExecutorService();
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

    /**
     * Retrieve an issue object associated with the given <code>URL</code>.
     *
     * @param url the <code>URL</code> of the issue to be retrieved.
     * @return the <code>Issue</code> associated with the provided <code>URK</code>.
     * @throws NotFoundException if the provided <code>URL</code> is not associated with an issue at any of the active issue
     * trackers.
     */
    public Issue getIssue(URL url) throws NotFoundException {
        Objects.requireNonNull(url, "url cannot be null");
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.getIssue(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Issue not found at IssueTrackerService: " + trackerService.getClass().getName());
            }
        }
        throw new NotFoundException("No issues found which correspond to url.");
    }

    /**
     * Retrieve all issues associated with the provided URLs. This method simply logs any issue URLs
     * that cannot be retrieved from a <code>IssueTrackerServer</code>. If the provided URLs
     * collection is empty, or no issues are found, then an empty List is returned.
     *
     * @param urls a collection of issue URLs.
     * @return a list of <code>Issue</code> objects associated with the provided urls.
     */
    public List<Issue> getIssues(Collection<URL> urls) {
        Objects.requireNonNull(urls, "the collection of urls cannot be null");

        if (urls.isEmpty())
            return new ArrayList<>();

        List<Callable<List<Issue>>> requests = new ArrayList<>(issueTrackers.size());
        for (IssueTrackerService tracker : issueTrackers)
            requests.add(() -> tracker.getIssues(urls));

        return getIssuesInParallel(requests);
    }

    /**
     * Return all issues, across all Issue Trackers, which match the passed <code>SearchCriteria</code>.
     *
     * @param searchCriteria all set fields will be search for.
     * @return a list of all <code>Issue</code> objects which match the specified searchCriteria,
     *         or an empty list if no issues match the searched criteria.
     */
    public List<Issue> searchIssues(SearchCriteria searchCriteria) {
        Objects.requireNonNull(searchCriteria, "searchCriteria cannot be null");
        checkIssueTrackerExists();

        List<Callable<List<Issue>>> searchRequests = new ArrayList<>(issueTrackers.size());
        for (IssueTrackerService tracker : issueTrackers)
            searchRequests.add(() -> tracker.searchIssues(searchCriteria));

        return getIssuesInParallel(searchRequests);
    }

    private List<Issue> getIssuesInParallel(List<Callable<List<Issue>>> callables) {
        List<Issue> issues = new ArrayList<>();
        try {
            List<Future<List<Issue>>> futures = executorService.invokeAll(callables);
            for (Future<List<Issue>> result : futures) {
                try {
                    issues.addAll(result.get());
                } catch (ExecutionException | CancellationException | InterruptedException e) {
                    if (LOG.isWarnEnabled())
                        LOG.warn("Exception encountered when processing request future: " + e);
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Issue search interrupted unexpectedly: " + e);
        }
        return issues;
    }

    /**
     * Return all issues which match the provided filter.
     *
     * @param filterUrl the url of the issue tracker filtered to be applied.
     * @return a list of all <code>Issue</code> objects which are returned by the provided filter.
     * @throws NotFoundException if the filterURL is not associated with any filters at any of the Issue Trackers.
     */
    public List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException {
        Objects.requireNonNull(filterUrl, "filterUrl cannot be null");
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
        throw new NotFoundException("No filter found which correspond to url.");
    }

    /**
     * Update a specific <code>Issue</code> at the remote issue tracker service.
     *
     * Note, this does not update issue comments or an issues description.
     * To add a new comment, use {@link #addCommentToIssue(Issue, Comment)}
     *
     * @param issue the issue to be updated at the associated <code>IssueTrackerService</code>
     * @return true if the issue was successfully updated, false otherwise.
     * @throws NotFoundException if the provided <code>Issue</code> cannot be found at the IssueTracker.
     * @throws AphroditeException if the user credentials supplied for this issue track do not have
     *                               permission to update this issue, or a field within this issue.
     */
    public boolean updateIssue(Issue issue) throws NotFoundException, AphroditeException {
        Objects.requireNonNull(issue, "issue cannot be null");
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.updateIssue(issue);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Issue not found at IssueTrackerService: " + trackerService.getClass().getName());
            }
        }
        throw new NotFoundException("No issues found which correspond to url.");
    }

    /**
     * Adds a new comment to the specified issue.
     *
     * @param issue the issue to add a new comment to.
     * @param comment the comment to be added to the issue.
     * @return true if the comment was successfully added to the issue, false otherwise.
     */
    public boolean addCommentToIssue(Issue issue, Comment comment) {
        Objects.requireNonNull(issue, "issue cannot be null");
        Objects.requireNonNull(comment, "comment cannot be null");
        checkIssueTrackerExists();

        for (IssueTrackerService trackerService : issueTrackers) {
            try {
                return trackerService.addCommentToIssue(issue, comment);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("IssueTrackerService: " + trackerService.getClass().getName() +
                            ": " + e);
            }
        }
        throw new IllegalStateException("Unable to add comment to issue: " + issue.getURL());
    }

    /**
     * Adds the <code>Comment</code> to the associated <code>Issue</code> object for all Issue/Comment
     * pairs in the <code>Map</code>. Null comments are ignored.
     *
     * @param commentMap the map containing all Issues that are to be updated and the associated comments.
     * @return true if all comments are successfully added to their associated Issue, otherwise false.
     */
    public boolean addCommentToIssue(Map<Issue, Comment> commentMap) {
        checkIssueTrackerExists();
        Objects.requireNonNull(commentMap, "commentMap cannot be null");

        boolean isSuccess = true;
        for (IssueTrackerService trackerService : issueTrackers) {
            if (!trackerService.addCommentToIssue(commentMap))
                isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * Adds the <code>Comment</code> to all of the provided <code>Issue</code> objects.
     *
     * @param issues a collection of all issues that the comment should be added to.
     * @param comment the comment to be added to all issues.
     * @return true if the comment is successfully added to all issues.
     */
    public boolean addCommentToIssue(Collection<Issue> issues, Comment comment) {
        checkIssueTrackerExists();
        Objects.requireNonNull(issues, "issues collection cannot be null");
        Objects.requireNonNull(comment, "comment cannot be null");

        boolean isSuccess = true;
        for (IssueTrackerService trackerService : issueTrackers) {
            if (!trackerService.addCommentToIssue(issues, comment))
                isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * Retrieve all Issues associated with the provided patch object.
     * Implementations of this method assume that the urls of the related issues are present in the
     * patch's description field.
     *
     * @param patch the <code>Patch</code> object whoms associated Issues should be returned.
     * @return a list of all <code>Issue</code> objects, or an empty list if no issues can be found.
     */
    public List<Issue> getIssuesAssociatedWith(Patch patch) {
        checkIssueTrackerExists();
        Objects.requireNonNull(patch, "patch cannot be null");

        return issueTrackers.stream()
                .map(service -> service.getIssuesAssociatedWith(patch))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get the repository located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the repository to be retrieved.
     * @return the <code>Repository</code> object.
     * @throws NotFoundException if a <code>Repository</code> cannot be found at the provided base url.
     */
    public Repository getRepository(URL url) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(url, "url cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getRepository(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("Repository not found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No repositories found which correspond to url.");
    }

    /**
     * Retrieve all Patches associated with the provided <code>Issue</code> object
     *
     * @param issue the <code>Issue</code> object whose associated Patches should be returned.
     * @return a list of all <code>Patch</code> objects, or an empty list if no patches can be found.
     */
    public List<Patch> getPatchesAssociatedWith(Issue issue) {
        checkRepositoryServiceExists();
        Objects.requireNonNull(issue, "issue cannot be null");

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

    /**
     * Retrieve all Patches associated with the provided <code>Repository</code> object, which have a
     * state that matches the provided <code>PatchState</code> object.
     *
     * @param repository the <code>Repository</code> object whose associated Patches should be returned.
     * @param state the <code>PatchState</code> which the returned <code>Patch</code> objects must have.
     * @return a list of all matching <code>Patch</code> objects, or an empty list if no patches can be found.
     */
    public List<Patch> getPatchesByState(Repository repository, PatchState state) {
        checkRepositoryServiceExists();
        Objects.requireNonNull(repository, "repository cannot be null");
        Objects.requireNonNull(state, "state cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getPatchesByState(repository, state);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Get the <code>Patch</code> located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the patch to be retrieved.
     * @return the <code>Patch</code> object.
     * @throws NotFoundException if a <code>Patch</code> cannot be found at the provided base url.
     */
    public Patch getPatch(URL url) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(url, "url cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getPatch(url);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No patch found which corresponds to patch.");
    }

    /**
     * Retrieve all labels associated with the provided <code>Patch</code> in <code>Repository</code> object.
     * @param patch the <code>Repository<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no label can be found.
     * @throws NotFoundException if the provided <code>Repository</code> url not consistent with the baseURL.
     */
    public List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(repository, "repository cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getLabelsFromRepository(repository);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No repository found at RepositoryService: " + repositoryService.getClass().getName(), e);

            }
        }

        return new ArrayList<>();
    }

    /**
     * Retrieve all labels associated with the provided <code>Patch</code> object.
     * @param patch the <code>Patch<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no patches can be found.
     * @throws if the provided <code>Patch</code> url not consistent with the baseURL.
     */
    public List<Label> getLabelsFromPatch(Patch patch) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                return repositoryService.getLabelsFromPatch(patch);
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at Patch: " + repositoryService.getClass().getName(), e);

            }
        }
        return new ArrayList<>();

    }

    /**
     * Set the labels for the provided <code>Patch</code> object.
     * @param patch the <code>Patch</code> object whose will be set.
     * @param labels the <code>Label</code> apply to the <code>Patch</code>
     * @throws NotFoundException if the <code>Label</code> can not be found in the provided <code>Patch</code>
     * @throws AphroditeException if add the <code>Label<code> is not consistent with get labels
     */
    public void setLabelsToPatch(Patch patch, List<Label> labels) throws NotFoundException, AphroditeException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");
        Objects.requireNonNull(labels, "labels cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                repositoryService.setLabelsToPatch(patch, labels);
                return;
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
    }

    /**
     *Delete a label from the provided <code>Patch</code> object.
     * @param patch the <code>Patch</code> whose label will be removed.
     * @param name the <code>Label</code> name will be removed.
     * @throws NotFoundException if the <code>Label</code> name can not be found in the provided <code>Patch</code>
     */
    public void removeLabelFromPatch(Patch patch, String name) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");
        Objects.requireNonNull(name, "labelname cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                repositoryService.removeLabelFromPatch(patch, name);
                return;
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
    }

    /**
     * Add a <code>Comment</code> to the specified <code>Patch</code> object, and propagate the changes
     * to the remote repository.
     *
     * @param patch the <code>Patch</code> on which the comment will be made.
     * @param comment the new <code>Comment</code>.
     * @throws NotFoundException if the <code>Patch</code> cannot be found at the remote repository.
     */
    public void addCommentToPatch(Patch patch, String comment) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");
        Objects.requireNonNull(comment, "comment cannot be null");

        for (RepositoryService repositoryService : repositories) {
            try {
                repositoryService.addCommentToPatch(patch, comment);
                return;
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        throw new NotFoundException("No patch found which corresponds to patch.");
    }

    /**
     * Attach a label to the specified patch.  Note the label must already exist at remote repository,
     * otherwise it will not be applied. If the specified label is already
     * associated with the provided patch then no further action is taken.
     *
     * @param patch the <code>Patch</code> to which the label will be applied.
     * @param labelName the name of the label to be applied.
     */
    public void addLabelToPatch(Patch patch, String labelName) {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");
        Objects.requireNonNull(labelName, "labelName cannot be null");

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

    public List<Patch> findPatchesRelatedTo(Patch patch) {
        checkRepositoryServiceExists();
        Objects.requireNonNull(patch, "patch cannot be null");

        List<Patch> patches = new ArrayList<Patch>();
        for (RepositoryService repositoryService : repositories) {
            try {
                patches.addAll(repositoryService.findPatchesRelatedTo(patch));
            } catch (NotFoundException e) {
                if (LOG.isInfoEnabled())
                    LOG.info("No patches found at RepositoryService: " + repositoryService.getClass().getName(), e);
            }
        }
        return patches;
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
