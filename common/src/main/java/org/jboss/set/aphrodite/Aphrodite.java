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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonReader;

import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.expr.SystemPropertyExpressionResolver;
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.issue.trackers.common.IssueCreationDetails;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.IssueTrackerService;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.RepositoryService;
import org.jboss.set.aphrodite.spi.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aphrodite implements AutoCloseable {

    public static final String FILE_PROPERTY = "aphrodite.config";

    private static final Logger LOG = LoggerFactory.getLogger(Aphrodite.class);
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
     * @throws AphroditeException if initialization fails
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
        issueTrackers.values().forEach(IssueTrackerService::destroy);
        issueTrackers.clear();
        repositories.forEach(RepositoryService::destroy);
        repositories.clear();
    }

    private final Map<String,IssueTrackerService> issueTrackers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<RepositoryService> repositories = new ArrayList<>();
    private final List<StreamService> streamServices = new ArrayList<>();

    private ScheduledExecutorService executorService;

    private AphroditeConfig config;

    private Aphrodite() throws AphroditeException {
        String propFileLocationProperty = System.getProperty(FILE_PROPERTY);
        String propFileLocation = propFileLocationProperty != null ? propFileLocationProperty : System.getenv().get(FILE_PROPERTY);

        if (propFileLocation == null)
            throw new IllegalArgumentException("Property '" + FILE_PROPERTY + "' must be set");

        try (JsonReader jr = Json.createReader(new FileInputStream(propFileLocation))) {
            init(AphroditeConfig.fromJson(new SystemPropertyExpressionResolver(), jr.readObject()));
        } catch (IOException e) {
            Utils.logException(LOG, "Unable to load file: " + propFileLocation, e);
            throw new AphroditeException(e);
        }
    }

    private Aphrodite(AphroditeConfig config) throws AphroditeException {
        init(config);
    }

    private void init(AphroditeConfig config) throws AphroditeException {
        if (LOG.isInfoEnabled())
            LOG.info("Initiating Aphrodite ...");

        boolean failed = false;
        StringBuilder error = new StringBuilder();
        this.config = config;
        SimpleContainer container = (SimpleContainer) SimpleContainer.instance();

        executorService = config.getExecutorService();
        // Create new config object, as the object passed to init() will have its state changed.
        AphroditeConfig mutableConfig = new AphroditeConfig(config);

        for (IssueTrackerService is : ServiceLoader.load(IssueTrackerService.class)) {
            boolean initialised = is.init(mutableConfig);
            if (initialised) {
                issueTrackers.put(is.getTrackerID(),is);
                container.register(is.getClass().getSimpleName(), is);
            } else if (AbstractIssueTracker.exists((AbstractIssueTracker) is)) {
                error.append("Failed to initialize issue tracker: ").append(is.getTrackerID()).append("\n");
                failed = true;
            }
        }

        for (RepositoryService rs : ServiceLoader.load(RepositoryService.class)) {
            boolean initialised = rs.init(mutableConfig);
            if (initialised) {
                repositories.add(rs);
            } else if (AbstractRepositoryService.exists((AbstractRepositoryService) rs)) {
                error.append("Failed to initialize repository: ").append(rs.getRepositoryType()).append("\n");
                failed = true;
            }
        }

        if (failed)
            throw new AphroditeException("Unable to initiatilise Aphrodite.\n" + error.toString());

        initialiseStreams(mutableConfig);

        int period = config.getStreamServiceUpdateRate();
        int initialDelay = config.getInitialDelay();
        if (period > 0) {
            this.executorService.scheduleAtFixedRate(new UpdateStreamServices(), initialDelay, config.getStreamServiceUpdateRate(), TimeUnit.MINUTES);
        }
        if (LOG.isInfoEnabled())
            LOG.info("Aphrodite Initialisation Complete");
    }

    private void initialiseStreams(AphroditeConfig mutableConfig) throws AphroditeException {
        if (mutableConfig.getStreamConfigs().isEmpty()) {
            return;
        }
        if (repositories.isEmpty()) {
            throw new AphroditeException("Unable to initialise any Stream Services as no " +
                    RepositoryService.class.getName() + " have been created.");
        }

        for (StreamService ss : ServiceLoader.load(StreamService.class)) {
            try {
                boolean initialised = ss.init(this, mutableConfig);
                if (initialised)
                    streamServices.add(ss);
            } catch (NotFoundException e) {
                throw new AphroditeException("Unable to initiatilise Aphrodite as an error was thrown when initiating "
                        + ss.getClass().getName() + ": " + e);
            }
        }
    }

    /**
     * Retrieve an issue object associated with the given <code>URL</code>.
     *
     * @param url the <code>URL</code> of the issue to be retrieved.
     * @return the <code>Issue</code> associated with the provided <code>URK</code>.
     * @throws NotFoundException if the provided <code>URL</code> is not associated with an issue at any of the active issuetrackers.
     *
     */
    public Issue getIssue(URI uri) throws NotFoundException {
        Objects.requireNonNull(uri, "url cannot be null");
        checkIssueTrackerExists();
        final IssueTrackerService its = getTrackerFor(uri);
        if(its != null){
           return its.getIssue(uri);
        }
        throw new NotFoundException("No tracker for issue url: " + uri);
    }

    /**
     * Create skeletal issue.
     * @param trackerURL - URL of tracker in which issue should be created
     * @param projectKey - ID of project/product
     * @param parameters - parameters, differ for each tracker.
     * @return
     * @throws NotFoundException
     * @throws MalformedURLException
     */
    public Issue createIssue(final IssueCreationDetails details) throws NotFoundException, URISyntaxException, AphroditeException {
        assert details != null;
        assert details.getTrackerURI() != null;
        final IssueTrackerService its = getTrackerFor(details.getTrackerURI());
        if(its != null){
            return its.createIssue(details);
         }
        throw new NotFoundException("No tracker for url: " + details.getTrackerURI());
    }

    /**
     * Retrieve all issues associated with the provided URLs. This method simply logs any issue URLs
     * that cannot be retrieved from a <code>IssueTrackerServer</code>. If the provided URLs
     * collection is empty, or no issues are found, then an empty List is returned.
     *
     * @param urls a collection of issue URLs.
     * @return a list of <code>Issue</code> objects associated with the provided urls.
     */
    public List<Issue> getIssues(Collection<URI> uris) {
        Objects.requireNonNull(uris, "the collection of urls cannot be null");

        if (uris.isEmpty())
            return new ArrayList<>();
        List<CompletableFuture<List<Issue>>> requests =
                issueTrackers.values().stream()
                        .map(tracker -> CompletableFuture.supplyAsync(() -> tracker.getIssues(uris), executorService))
                        .collect(Collectors.toList());

        return requests.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
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

        if (searchCriteria.isEmpty())
            return new ArrayList<>();

        List<CompletableFuture<List<Issue>>> searchRequests =
                issueTrackers.values().stream()
                        .map(tracker -> CompletableFuture.supplyAsync(() -> tracker.searchIssues(searchCriteria), executorService))
                        .collect(Collectors.toList());

        return searchRequests.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Return all issues which match the provided filter.
     *
     * @param filterUrl the url of the issue tracker filtered to be applied.
     * @return a list of all <code>Issue</code> objects which are returned by the provided filter.
     * @throws NotFoundException if the filterURL is not associated with any filters at any of the Issue Trackers.
     */
    public List<Issue> searchIssuesByFilter(URI filterUri) throws NotFoundException {
        Objects.requireNonNull(filterUri, "filterUrl cannot be null");
        checkIssueTrackerExists();

        final IssueTrackerService its = getTrackerFor(filterUri);
        if(its != null){
           return its.searchIssuesByFilter(filterUri);
        }

        throw new NotFoundException("No filter found which correspond to url: " + filterUri);
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

        final IssueTrackerService its = getTrackerFor(issue.getURI());
        if(its != null){
           return its.updateIssue(issue);
        }

        throw new NotFoundException("No issues found which correspond to url: " + issue.getURI());
    }

    /**
     * Adds a new comment to the specified issue.
     *
     * @param issue the issue to add a new comment to.
     * @param comment the comment to be added to the issue.
     */
    public void addCommentToIssue(Issue issue, Comment comment) throws NotFoundException {
        Objects.requireNonNull(issue, "issue cannot be null");
        Objects.requireNonNull(comment, "comment cannot be null");
        checkIssueTrackerExists();

        final IssueTrackerService its = getTrackerFor(issue.getURI());
        if(its != null){
            its.addCommentToIssue(issue, comment);
            return;
        }

        throw new NotFoundException("No issues found which correspond to url: " + issue.getURI());
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
        for(Entry<Issue, Comment> ie:commentMap.entrySet()){
            final IssueTrackerService its = getTrackerFor(ie.getKey().getURI());
            if(its != null){
                try {
                    its.addCommentToIssue(ie.getKey(), ie.getValue());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    isSuccess = false;
                }
            } else {
                isSuccess = false;
            }
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
        for (Issue i : issues) {
            final IssueTrackerService its = getTrackerFor(i.getURI());
            if (its != null) {
                try {
                    its.addCommentToIssue(i, comment);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    isSuccess = false;
                }
            } else {
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    /**
     * Get the repository located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the repository to be retrieved.
     * @return the <code>Repository</code> object.
     * @throws NotFoundException if a <code>Repository</code> cannot be found at the provided base url,
     * or no service exists with the same host domain as the provided URL.
     */
    public Repository getRepository(URI uri) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(uri, "url cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.repositoryAccessable(uri) && repositoryService.uriExists(uri))
                return repositoryService.getRepository(uri);
        }
        throw new NotFoundException("No repositories found which correspond to url: " + uri);
    }

    /**
     * Retrieve all PullRequests associated with the provided <code>Repository</code> object, which have a
     * state that matches the provided <code>PullRequestState</code> object.
     *
     * @param repository the <code>Repository</code> object whose associated PullRequests should be returned.
     * @param state the <code>PullRequestState</code> which the returned <code>PullRequest</code> objects must have.
     * @return a list of all matching <code>PullRequest</code> objects, or an empty list if no pullRequests can be found.
     * @throws NotFoundException if an exception is encountered when trying to retrieve pullRequests from a RepositoryService
     */
    public List<PullRequest> getPullRequestsByState(Repository repository, PullRequestState state) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(repository, "repository cannot be null");
        Objects.requireNonNull(state, "state cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.uriExists(repository.getURI()))
                return repositoryService.getPullRequestsByState(repository, state);
        }
        return Collections.emptyList();
    }

    /**
     * Get the <code>PullRequest</code> located at the provided <code>URL</code>.
     *
     * @param url the <code>URL</code> of the pullRequest to be retrieved.
     * @return the <code>PullRequest</code> object.
     * @throws NotFoundException if a <code>PullRequest</code> cannot be found at the provided base url.
     */
    public PullRequest getPullRequest(URI uri) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(uri, "url cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.uriExists(uri) && repositoryService.repositoryAccessable(uri))
                return repositoryService.getPullRequest(uri);
        }
        throw new NotFoundException("No pull request found which corresponds to url: " + uri);
    }

    public Map<RepositoryType, RateLimit> getRateLimits() throws NotFoundException {
        Map<RepositoryType, RateLimit> rateLimits = new HashMap<>();
        for (RepositoryService repositoryService : repositories) {
            RepositoryType repositoryType = repositoryService.getRepositoryType();
            RateLimit requestLimit = repositoryService.getRateLimit();
            rateLimits.put(repositoryType, requestLimit);
        }
        return Collections.unmodifiableMap(rateLimits);
    }

    /**
     * Retrieve all labels associated with the provided <code>PullRequest</code> in <code>Repository</code> object.
     * @param repository the <code>Repository<code> object whose associated labels should be returned.
     * @return a list of all matching <code>Label<code> objects, or an empty list if no labels can be found.
     * @throws NotFoundException if an error is encountered when trying to retrieve labels from a RepositoryService
     */
    public List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(repository, "repository cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.uriExists(repository.getURI()))
                return repositoryService.getLabelsFromRepository(repository);
        }
        return Collections.emptyList();
    }

    /**
     * Discover if the user logged into a <code>RepositoryService</code> has the correct permissions to apply/remove
     * labels to pull request in the provided <code>Repository</code>
     *
     * @param repository the <code>Repository</code> whose permissions are to be checked
     * @return true if the user has permission, otherwise false.
     * @throws NotFoundException if the specified <code>Repository</code> cannot be found.
     */
    public boolean isRepositoryLabelsModifiable(Repository repository) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(repository, "repository cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.uriExists(repository.getURI()))
                return repositoryService.hasModifiableLabels(repository);
        }
        throw new NotFoundException("No repository found which corresponds to url: " + repository.getURI());
    }

    /**
     * Returns the streams discovered by all of the active StreamServices
     * @return a list of all streams discovered by all <code>StreamService</code> instances.
     */
    public List<Stream> getAllStreams() {
        checkStreamServiceExists();

        return streamServices.stream()
                .flatMap(streamService -> streamService.getStreams().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get a specific <code>Stream</code> object based upon its String name.
     *
     * @param streamName the name of the <code>Stream</code> to be returned.
     * @return Stream the first <code>Stream</code> object which corresponds to the specified streamName
     *                if it exists at a StreamService.
     * @throws NotFoundException if the specified streamName does not exist at any of the loaded StreamServices.
     */
    public Stream getStream(String streamName) throws NotFoundException {
        checkStreamServiceExists();
        Objects.requireNonNull(streamName, "stream name can not be null");

        for (StreamService ss : streamServices) {
            Stream stream = ss.getStream(streamName);
            if (stream != null)
                return stream;
        }
        throw new NotFoundException("No Stream exists with the name '" + streamName + "'");
    }

    /**
     * Check if a given CP version is released.
     *
     * @param cpVersion the CP version to be tested. Jira accepts GA version format x.y.z.GA, e.g. 7.1.2.GA. Bugzilla accepts version format x.y.z, e.g. 6.4.18.
     * @return true if the given version is released, otherwise false.
     *
     */
    public boolean isCPReleased(String cpVersion) {
        Objects.requireNonNull(cpVersion, "CP version cannot be null");
        boolean released;
        checkIssueTrackerExists();
        // No ideal means to find proper issue tracker by version except doing hard code EAP6/7 <-> BZ/JIRA.
        // This is a blind match to both issue trackers.
        released = issueTrackers.values().stream().anyMatch(e -> e.isCPReleased(cpVersion));
        return released;
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

    private void checkStreamServiceExists(){
        if(streamServices.isEmpty())
            throw new IllegalStateException("Unable to retrieve streamas a valid " +
                    StreamService.class.getName() + " has not been created.");
    }

    private class UpdateStreamServices implements Runnable{

        @Override
        public void run() {
            if (LOG.isInfoEnabled())
                LOG.info("Update Aphrodite streams");
            for(StreamService ss: streamServices){
                try {
                    ss.updateStreams();
                } catch (NotFoundException e) {
                    if(LOG.isErrorEnabled()){
                        LOG.error("Failed to update stream service: "+ss, e);
                    }
                }
            }
            if (LOG.isInfoEnabled())
                LOG.info("Aphrodite streams update complete");
        }
    }

    public IssueTrackerService getTrackerFor(final URI uri){
        final String id = AbstractIssueTracker.convertToTrackerID(uri);
        if(this.issueTrackers.containsKey(id)){
           return this.issueTrackers.get(id);
        }
        return null;
    }

    public AphroditeConfig getConfig() {
        // allow to get configuration to initialize service outside Aphrodite
        return config;
    }

    /**
     * Return all the referenced PRs to a given PR using all the repositories
     * defined in the instance. All of them are called in order to get the
     * references for all of them.
     *
     * @param pullRequest The PR to obtain the references PRs
     * @return The list of PRs referenced by this PR
     */
    public List<PullRequest> findReferencedPullRequests(PullRequest pullRequest) {
        checkRepositoryServiceExists();
        List<PullRequest> res = new ArrayList<>();
        for (RepositoryService repositoryService : repositories) {
            PullRequestHome prHome = repositoryService.getPullRequestHome();
            res.addAll(prHome.findReferencedPullRequests(pullRequest));
        }
        return res;
    }

    /**
     * Returns a list of commits on a given branch between a given date and now
     *
     * @param url URL of the <code>Repository</code>
     * @param branch branch in the <code>Repository</code>
     * @param since date in milliseconds
     * @return List of commits past the given date
     * @throws NotFoundException if the specified <code>Repository</code> cannot be found.
     */
    public List<Commit> getCommitsSince(URI uri, String branch, long since) throws NotFoundException {
        checkRepositoryServiceExists();
        Objects.requireNonNull(uri, "url cannot be null");
        Objects.requireNonNull(branch, "branch cannot be null");

        for (RepositoryService repositoryService : repositories) {
            if (repositoryService.uriExists(uri) && repositoryService.repositoryAccessable(uri))
                return repositoryService.getCommitsSince(uri, branch, since);
        }
        throw new NotFoundException("No pull request found which corresponds to url: " + uri);
    }
}