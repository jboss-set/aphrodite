/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.repository.services.github;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.createRepositoryIdFromUrl;
import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.getPRFromDescription;

import static org.jboss.set.aphrodite.repository.services.github.GithubUtils.getCombineStatus;

/**
 * Service implementation of {@link PullRequestHome}. This helps to detach pull request specific methods in
 * {@link GitHubRepositoryService} and allow to call them from pull request itself once this service implementation registered
 * in container.
 */
public class GithubPullRequestHomeService extends AbstractRepositoryService implements PullRequestHome {
    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.repository.services.github.GithubPullRequestHomeService.class);
    private static final GitHubWrapper WRAPPER = new GitHubWrapper();
    private static final int DEFAULT_CACHE_SIZE = 20;

    private String cacheDir;
    private String cacheName;
    private String cacheSize;
    private File cacheFile;
    private Cache cache;
    private GitHub github;

    public GithubPullRequestHomeService(Aphrodite aphrodite) {
        super(RepositoryType.GITHUB);
        AphroditeConfig configuration = aphrodite.getConfig();
        this.init(configuration);
    }

    public boolean init(RepositoryConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated) {
            return false;
        }

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
                github = new GitHubBuilder().withOAuthToken(config.getPassword(), config.getUsername())
                        .withConnector(new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache)))).build();

            }
            return github.isCredentialValid();
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for RepositoryService: " + this.getClass().getName(), e);
        }
        return false;
    }

    @Override
    public List<PullRequest> findReferencedPullRequests(PullRequest pullRequest) {
        try {
            List<URL> urls = getPRFromDescription(pullRequest.getURL(), pullRequest.getBody());
            List<PullRequest> referencedPullRequests = new ArrayList<>();
            for (URL url : urls) {
                // Only try and retrieve pull request if it is located on the same host as this service
                if (url.getHost().equals(baseUrl.getHost())) {
                    PullRequest validPullRequest = getPullRequest(url);
                    if (validPullRequest != null) {
                        referencedPullRequests.add(getPullRequest(url));
                    }
                } else {
                    Utils.logWarnMessage(LOG, "Unable to process url '" + url + "' as it is not located on this service");
                }
            }
            return referencedPullRequests;
        } catch (MalformedURLException | URISyntaxException e) {
            Utils.logException(LOG, "error to get referenced pull requests to " + pullRequest.getURL(), e);
            return Collections.emptyList();
        }
    }

    private PullRequest getPullRequest(URL url) {
        String[] elements = url.getPath().split("/");
        int pullId = Integer.parseInt(elements[elements.length - 1]);
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest pullRequest = repository.getPullRequest(pullId);
            return WRAPPER.pullRequestToPullRequest(pullRequest);
        } catch (IOException e) {
            Utils.logException(LOG, "Unable to retrieve pull request from url " + url, e);
            return null;
        }
    }

    @Override
    public boolean addComment(PullRequest pullRequest, String comment) {
        URL url = pullRequest.getURL();

        int id = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(id);
            issue.comment(comment);
            return true;
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
    }

    @Override
    public List<Label> getLabels(PullRequest pullRequest) {
        URL url = pullRequest.getURL();
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(Integer.parseInt(pullRequest.getId()));
            return WRAPPER.pullRequestLabeltoPullRequestLabel(issue.getLabels());
        } catch (IOException | NumberFormatException e) {
            Utils.logException(LOG, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean setLabels(PullRequest pullRequest, List<Label> labels) {
        URL url = pullRequest.getURL();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            List<GHLabel> issueLabels = new ArrayList<>();
            List<GHLabel> existingLabels = repository.listLabels().asList();

            for (Label label : labels) {
                GHLabel validLabel = validAndGetLabel(repository, label, existingLabels);
                if(validLabel != null) {
                    issueLabels.add(validLabel);
                }
            }
            List<String> list = issueLabels.stream().map(e -> e.getName()).collect(Collectors.toList());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean addLabel(PullRequest pullRequest, Label label) {
        URL url = pullRequest.getURL();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);

        try {
            GHRepository repository = github.getRepository(repositoryId);
            List<GHLabel> existingLabels = repository.listLabels().asList();
            GHLabel newLabel = validAndGetLabel(repository, label, existingLabels);
            if (newLabel == null) {
                Utils.logWarnMessage(LOG, "No label exists with name '" + label.getName() + "' at repository '" + repository.getName() + "'");
                return false;
            }
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();
            if (labels.contains(newLabel)) {
                return true; // label is already existed.
            }

            List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
            list.add(newLabel.getName());
            String[] labelArray = list.toArray(new String[list.size()]);
            issue.setLabels(labelArray);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        return true;
    }

    private GHLabel validAndGetLabel(GHRepository repository, Label label, List<GHLabel> existingLabels) throws IOException {
        for (GHLabel exsitingLabel : existingLabels) {
            if (exsitingLabel.getName().equalsIgnoreCase(label.getName()))
                return exsitingLabel;
        }
        return null;
    }

    @Override
    public boolean removeLabel(PullRequest pullRequest, Label label) {
        URL url = pullRequest.getURL();
        String labelName = label.getName();
        int pullRequestId = new Integer(Utils.getTrailingValueFromUrlPath(url));
        String repositoryId = createRepositoryIdFromUrl(url);

        try {
            GHRepository repository = github.getRepository(repositoryId);
            GHIssue issue = repository.getIssue(pullRequestId);
            Collection<GHLabel> labels = issue.getLabels();
            for (GHLabel l : labels)
                if (l.getName().equalsIgnoreCase(labelName)) {
                    // remove the label and reset
                    List<String> list = labels.stream().map(e -> e.getName()).collect(Collectors.toList());
                    list.remove(l.getName());
                    String[] labelArray = list.toArray(new String[list.size()]);
                    issue.setLabels(labelArray);
                    return true;
                }
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return false;
        }
        Utils.logWarnMessage(LOG, "No label exists with name '" + labelName + "' at repository '" + repositoryId + "'");
        return false;
    }

    @Override
    public CommitStatus getCommitStatus(PullRequest pullRequest) {
        URL url = pullRequest.getURL();
        CommitStatus status = null;
        int pullRequestId = Integer.parseInt(pullRequest.getId());
        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            String sha = null;

            GHRepository repository = github.getRepository(repositoryId);
            GHPullRequest ghPullRequest = repository.getPullRequest(pullRequestId);

            List<GHPullRequestCommitDetail> commits = ghPullRequest.listCommits().asList();
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
            return CommitStatus.UNKNOWN;
        }

        if (status != null) {
            return status;
        } else {
            return CommitStatus.UNKNOWN;
        }
    }

    public boolean repositoryAccessable(URL url) {
        if (url.toString().contains("svn.jboss.org")) {
            // svn repository is not supported
            Utils.logWarnMessage(LOG, "svn repository : " + url + " is not supported.");
            return false;
        }

        String repositoryId = createRepositoryIdFromUrl(url);
        try {
            GHRepository repository = github.getRepository(repositoryId);
            repository.getBranches(); // action to test account repository accessibility
        } catch (IOException e) {
            Utils.logWarnMessage(LOG, "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return false;
        }
        return true;
    }

    @Override
    protected Log getLog() {
        return LOG;
    }
}
