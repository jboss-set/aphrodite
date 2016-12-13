/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;


/**
 * @author Ryan Emerson
 */
public class GitHubRepositoryService extends AbstractRepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.spi.RepositoryService.class);
    private final GitHubWrapper WRAPPER = new GitHubWrapper();
    private static final int DEFAULT_CACHE_SIZE = 20;

    private String cacheDir;
    private String cacheName;
    private String cacheSize;
    private File cacheFile;
    private Cache cache;
    private GitHub github;

    public GitHubRepositoryService() {
        super(RepositoryType.GITHUB);
    }

    @Override
    protected Log getLog() {
        return LOG;
    }

    @Override
    public boolean init(RepositoryConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        // Cache
        cacheDir = System.getProperty("cacheDir");
        cacheName = System.getProperty("cacheName");

        try {
            if (cacheDir == null || cacheName == null) {
                // no cache specified
                github = GitHub.connect(config.getUsername(), config.getPassword());
            } else {
                // use cache
                cacheFile = new File(cacheDir, cacheName);
                cacheSize = System.getProperty("cacheSize");
                if (cacheSize == null) {
                    cache = new Cache(cacheFile, DEFAULT_CACHE_SIZE * 1024 * 1024); // default 20MB cache
                } else {
                    int size = DEFAULT_CACHE_SIZE;
                    try {
                        size = Integer.valueOf(cacheSize);
                    } catch (NumberFormatException e) {
                        Utils.logWarnMessage(LOG, cacheSize + " is not a valid cache size. Use default size 20MB.");
                    }
                    cache = new Cache(cacheFile, size * 1024 * 1024); // default 20MB cache
                }

                // oauthAccessToken here, if you use text password, call .withPassword()
                github = new GitHubBuilder()
                        .withOAuthToken(config.getPassword(), config.getUsername())
                        .withConnector(new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache))))
                        .build();

            }
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for RepositoryService: " + this.getClass().getName(), e);
            return false;
        }
        return true;
    }

    @Override
    public Patch getPatch(URL url) throws NotFoundException {
        checkHost(url);

        String[] elements = url.getPath().split("/");
        int pullId = Integer.parseInt(elements[elements.length - 1]);
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest pullRequest = repository.getPullRequest(pullId);
            return WRAPPER.pullRequestToPatch(pullRequest);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public Repository getRepository(URL url) throws NotFoundException {
        checkHost(url);

        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            Collection<GHBranch> branches = repository.getBranches().values();
            return WRAPPER.toAphroditeRepository(url, branches);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

//    @Override
//    public List<Patch> getPatchesAssociatedWith(Issue issue) throws NotFoundException {
//        String trackerId = issue.getTrackerId().orElseThrow(() -> new IllegalArgumentException("Issue.trackerId must be set."));
//        try {
//            GitHubGlobalSearchService searchService = new GitHubGlobalSearchService(gitHubClient);
//            List<SearchResult> searchResults = searchService.searchAllPullRequests(trackerId);
//            return searchResults.stream()
//                    .map(pr -> getPatch(pr.getUrl()))
//                    .filter(patch -> patch != null)
//                    .collect(Collectors.toList());
//        } catch (IOException e) {
//            Utils.logException(LOG, e);
//            throw new NotFoundException(e);
//        }
//    }

    @Override
    public List<Patch> getPatchesByState(Repository repository, PatchState state) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createFromUrl(url);
        try {
            // String githubState = state.toString().toLowerCase();
            GHRepository githubRepository = github.getRepository(repositoryId);
            GHIssueState issueState;
            try {
                issueState = GHIssueState.valueOf(state.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                issueState = GHIssueState.OPEN;
            }
            List<GHPullRequest> pullRequests = githubRepository.getPullRequests(issueState);
            return WRAPPER.toAphroditePatches(pullRequests);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public void addCommentToPatch(Patch patch, String comment) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int id = Integer.parseInt(patch.getId());
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(id);
            issue.comment(comment);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public boolean hasModifiableLabels(Repository repository) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createFromUrl(url);
        try {
            GHMyself myself = github.getMyself();
            GHRepository githubRepository = github.getRepository(repositoryId);
            Set<GHUser> collaborators = githubRepository.listCollaborators().asSet();
            return collaborators.stream().anyMatch(e -> e.getLogin().equals(myself.getLogin()));
        } catch (Throwable t) {
            if (t.getMessage().contains("Must have push access")) {
                return false;
            }
            Exception e = (Exception) t;
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public void addLabelToPatch(Patch patch, String labelName) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int patchId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHLabel newLabel = getLabel(repository, labelName);
            GHIssue issue = repository.getIssue(patchId);
            Collection<GHLabel> labels = issue.getLabels();
            if (labels.contains(newLabel)) {
                return;
            }

            List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
            list.add(newLabel.getName());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    private GHLabel getLabel(GHRepository repository, String labelName) throws NotFoundException, IOException {
        List<GHLabel> labels = repository.listLabels().asList();
        return getLabel(repository, labelName, labels);
    }

    private GHLabel getLabel(GHRepository repository, String labelName, List<GHLabel> validLabels) throws NotFoundException {
        for (GHLabel label : validLabels) {
            if (label.getName().equalsIgnoreCase(labelName))
                return label;
        }
        throw new NotFoundException("No label exists with the name '" + labelName +
                "' at repository '" + repository.getName() + "'");
    }

    @Override
    public List<Label> getLabelsFromRepository(Repository repository) throws NotFoundException {
        URL url = repository.getURL();
        checkHost(url);

        String repositoryId = createFromUrl(url);
        List<GHLabel> labels;
        try {
            GHRepository githubRepository = github.getRepository(repositoryId);
            labels = githubRepository.listLabels().asList();
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }

        return WRAPPER.pullRequestLabeltoPatchLabel(labels);
    }

    @Override
    public List<Label> getLabelsFromPatch(Patch patch) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(Integer.parseInt(patch.getId()));
            return WRAPPER.pullRequestLabeltoPatchLabel(issue.getLabels());
        } catch (IOException | NumberFormatException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public void setLabelsToPatch(Patch patch, List<Label> labels) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int patchId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(patchId);
            List<GHLabel> issueLabels = new ArrayList<>();
            List<GHLabel> existingLabels = repository.listLabels().asList();

            for (Label label : labels) {
                issueLabels.add(getLabel(repository, label.getName(), existingLabels));
            }
            issueLabels.add(existingLabels.get(0));
            List<String> list = issueLabels.stream().map(e -> e.getName()).collect(Collectors.toList());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public void removeLabelFromPatch(Patch patch, String name) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        int patchId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(patchId);
            Collection<GHLabel> labels = issue.getLabels();

            for (GHLabel label : labels)
                if (label.getName().equalsIgnoreCase(name)) {
                    // remove the label and reset
                    List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
                    list.remove(label.getName());
                    String[] labelArray = list.toArray(new String[list.size()]);
                    issue.setLabels(labelArray);
                    return;
                }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
        throw new NotFoundException("No label exists with the name '" + name +
                "' at repository '" + repositoryId + "'");
    }

    private static final Pattern RELATED_PR_PATTERN = Pattern
            .compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN = Pattern.compile("([a-zA-Z_0-9-//]*)#(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO = Pattern
            .compile("([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)#(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<Patch> findPatchesRelatedTo(Patch patch) {
        try {
            List<URL> urls = getPRFromDescription(patch.getURL(), patch.getBody());
            List<Patch> related = new ArrayList<>();
            for (URL url : urls) {
                try {
                    // Only try and retrieve patch if it is located on the same host as this service
                    if (urlExists(url)) {
                        related.add(getPatch(url));
                    } else {
                        Utils.logWarnMessage(LOG, "Unable to process url '" + url + "' as it is not located on this service");
                    }
                } catch (NotFoundException e) {
                    Utils.logException(LOG, "Unable to retrieve url '" + url + "' referenced in the patch at: " + patch.getURL(), e);
                }
            }
            return related;
        } catch (MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "something went wrong while trying to get related patches to " + patch.getURL(), e);
            return Collections.emptyList();
        }
    }

    private List<URL> getPRFromDescription(URL url, String content) throws MalformedURLException, URISyntaxException {
        String[] paths = url.getPath().split("/");
        Matcher matcher = RELATED_PR_PATTERN.matcher(content);
        List<URL> relatedPullRequests = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                URL relatedPullRequest = new URI(
                        "https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pulls/" + matcher.group(3))
                                .toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        Matcher abbreviatedMatcher = ABBREVIATED_RELATED_PR_PATTERN.matcher(content);
        while (abbreviatedMatcher.find()) {
            String match = abbreviatedMatcher.group();
            Matcher abbreviatedExternalMatcher = ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO.matcher(match);
            if (abbreviatedExternalMatcher.find()) {
                if (abbreviatedExternalMatcher.groupCount() == 3) {
                    URL relatedPullRequest = new URI("https://github.com/"
                            + abbreviatedExternalMatcher.group(1) + "/"
                            + abbreviatedExternalMatcher.group(2) + "/pulls/"
                            + abbreviatedExternalMatcher.group(3)).toURL();
                    relatedPullRequests.add(relatedPullRequest);
                    continue;
                }
            }

            if (abbreviatedMatcher.groupCount() == 2) {
                URL relatedPullRequest = new URI(
                        "https://github.com/" + paths[1] + "/" + paths[2] + "/" + "/pulls/" + abbreviatedMatcher.group(2))
                                .toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        return relatedPullRequests;
    }

    @Override
    public CommitStatus getCommitStatusFromPatch(Patch patch) throws NotFoundException {
        URL url = patch.getURL();
        checkHost(url);

        CommitStatus status = null;
        int patchId = Integer.parseInt(patch.getId());
        String repositoryId = createFromUrl(url);
        try {
            String sha = null;

            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest pullRequest = repository.getPullRequest(patchId);

            List<GHPullRequestCommitDetail> commits = pullRequest.listCommits().asList();
            if (commits.size() > 0) {
                sha = commits.get(commits.size() - 1).getSha();
            }

            // statuses contains Finished and Started TeamCity Build
            List<GHCommitStatus> statuses = repository.listCommitStatuses(sha).asList();
            if (statuses.size() > 0) {
                GHCommitState sta = getCombineStatus(statuses);
                if (sta != null)
                    status = CommitStatus.fromString(sta.toString());
            }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }

        if (status != null) {
            return status;
        } else {
            return CommitStatus.UNKNOWN;
        }
    }

    private GHCommitState getCombineStatus(List<GHCommitStatus> comStatuses) {
        int count = 0, flag = 0;
        List<GHCommitState> stas = new ArrayList<>();
        for (GHCommitStatus status : comStatuses) {
            GHCommitState sta = status.getState();
            stas.add(sta);
            // until sta="pending"
            if (!sta.equals(GHCommitState.PENDING)) {
                if (sta.equals(GHCommitState.FAILURE)) {
                    return GHCommitState.FAILURE;
                } else if (sta.equals(GHCommitState.ERROR)) {
                    return GHCommitState.ERROR;
                }
            } else {
                flag = 1;
                // The Travis CI and TeamCity Build has different rules
                String description = status.getDescription();
                if (description != null && description.contains("Travis")) {
                    return stas.contains(GHCommitState.SUCCESS) ? GHCommitState.SUCCESS : GHCommitState.PENDING;
                }
                if (comStatuses.size() > 2 * count) {
                    GHCommitState temp = comStatuses.get(2 * count).getState();
                    return temp.equals(GHCommitState.PENDING) ? GHCommitState.PENDING : GHCommitState.SUCCESS;
                } else if (comStatuses.size() == 2 * count) {
                    return GHCommitState.SUCCESS;
                }
            }
            count++;
        }

        return (flag == 0) ? GHCommitState.SUCCESS : null;
    }

    @Override
    public boolean repositoryAccessable(URL url) {
        if (url.toString().contains("svn.jboss.org")) {
            // svn repository is not supported
            Utils.logWarnMessage(LOG, "svn repository : " + url + " is not supported.");
            return false;
        }

        String repositoryId = createFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            repository.getBranches(); // action to test account repository accessibility
        } catch (IOException e) {
            Utils.logWarnMessage(LOG,
                    "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return false;
        }
        return true;
    }

    public GHRateLimit getRateLimit() throws NotFoundException {
        try {
            return github.getRateLimit();
        } catch (IOException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    @Override
    public RepositoryType getRepositoryType() {
        return REPOSITORY_TYPE;
    }
}
