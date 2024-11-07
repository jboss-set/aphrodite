/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.repository.services.gitlab;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.MergeableState;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * <p>Utility methods for the gitlab repository.</p>
 *
 * @author rmartinc
 */
public class GitLabUtils {

    private static final Log LOG = LogFactory.getLog(GitLabUtils.class);

    /**
     * Return the project id from a gitlab repo URL. The URL is in the form:
     * <em>http(s)://hostname:port/group/project</em> and the method returns
     * the part <em>group/project</em>.
     *<p>
     * For More information: <a href="https://gitlab.com/gitlab-org/gitlab/-/issues/214217">
     *     Changes in GitLab routing</a>
     *</p>
     * @param url The gitab repo URL
     * @return The project id in the form <em>group/project</em>
     */
    public static String getProjectIdFromURL(URL url) {
        try {
            url = url.toURI().normalize().toURL();
            String[] path = url.getPath().split("/");
            String projectId = null;
            boolean done = false;
            for (int i = 0; i < path.length && !done; i++) {
                if (!path[i].isEmpty()) {
                    if ("-".equals(path[i])) {
                        done = true;
                    } else {
                        projectId = projectId == null ? path[i] : projectId + "/" + path[i];
                    }
                }
            }
            return projectId;
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.debug(url + "is not a valid URL", e);
            return null;
        }
    }

    /**
     * Return the projectId and the last path part in a two sized array. The
     * first string will be the project ID and the second the last path.
     * <p>
     * For the URL <em>http(s)://hostname:port/group/project/.../32</em> the method
     * returns the array <em>["group/project", "32"]</em>.
     * </p>
     * <p>
     * For the URL <em>http(s)://hostname:port/group/subgroup/project/.../32</em> the method
     * returns the array <em>["group/subgroup/project", "32"]</em>.
     * </p>
     * For More information: <a href="https://gitlab.com/gitlab-org/gitlab/-/issues/214217">
     *     Changes in GitLab routing</a>
     * @param url The URL to parse
     * @return The array with two parts or null
     */
    public static String[] getProjectIdAndLastFieldFromURL(URL url) {
        try {
            url = url.toURI().normalize().toURL();
            String[] path = url.getPath().split("/");
            String projectId = null;
            boolean done = false;
            int idx = -1;
            for (int i = 0; i < path.length && !done; i++) {
                if (!path[i].isEmpty()) {
                    if ("-".equals(path[i])) {
                        done = true;
                        idx = i;
                    } else {
                        projectId = projectId == null ? path[i] : projectId + "/" + path[i];
                    }
                }
            }
            String mergeId = null;
            if (idx != -1) {
                for (int i = path.length - 1; i > idx && mergeId == null; i--) {
                    if (!path[i].isEmpty()) {
                        mergeId = path[i];
                    }
                }
            }
            if (projectId != null && mergeId != null) {
                return new String[]{projectId, mergeId};
            } else {
                return null;
            }
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.debug(url + "is not a valid URL", e);
            return null;
        }
    }

    /**
     * Method to check if an URL is in the repo URL. The method checks the
     * protocol, hostname and port of both URLS.
     * @param url The URL to check
     * @param repoUrl The repository URL
     * @return true if the URL is in the same origin than the repo URL
     */
    public static boolean urlIsInRepo(URL url, URL repoUrl) {
        Objects.requireNonNull(url);
        return url.getProtocol().equals(repoUrl.getProtocol()) &&
                url.getHost().equalsIgnoreCase(repoUrl.getHost()) &&
                url.getPort() == repoUrl.getPort();
    }

    /**
     * If the url is not in the repoURL a NotFoundException is thrown.
     *
     * @param url The URL to check
     * @param repoUrl The repository URL
     * @throws NotFoundException If url is not in repoUrl
     */
    public static void checkIsInRepo(URL url, URL repoUrl) throws NotFoundException {
        if (!urlIsInRepo(url, repoUrl)) {
            throw new NotFoundException("Repository " + url + " cannot be found as it is not hosted on this server.");
        }
    }

    /**
     * Converts a gitlab state into a PullRequestState
     * Valid gitlab states: opened, closed, locked or merged
     *
     *
     * @param state The gitlab state
     * @return The aphrodite state
     */
    public static PullRequestState toPullRequestState(String state) {
        switch (state) {
            case "opened":
                return PullRequestState.OPEN;
            case "closed":
            case "merged":
                return PullRequestState.CLOSED;
            default:
                return PullRequestState.UNDEFINED;
        }
    }

    /**
     * Returns if the merge status is a mergeable aphrodite status.
     *
     * @param mergeStatus The gitlab merge status
     * @return true is it's mergeable
     */
    public static boolean toMergeable(String mergeStatus) {
        return "can_be_merged".equals(mergeStatus);
    }

    /**
     * Converts a gitlan commit status into an aphrodite commit status.
     *
     * @param statusValue The gitlab status of the commit
     * @return The equivalent status in aphrodite
     */
    public static CommitStatus toCommitStatus(String statusValue) {
        // pending, running, success, failed, canceled
        switch (statusValue) {
            case "pending":
                return CommitStatus.PENDING;
            case "running":
                return CommitStatus.PENDING;
            case "success":
                return CommitStatus.SUCCESS;
            case "failed":
                return CommitStatus.FAILURE;
            default:
                return CommitStatus.UNKNOWN;
        }
    }

    /**
     * Converts a gitlab merge object into an aphrodite pull request.
     *
     * @param m The gilab merge object
     * @param commits The commits of the merge request
     * @param url The URL for the merge
     * @param repo The repository of the merge
     * @param prHome The pull request home to use in the or
     * @return The aphrodite PullRequesy
     */
    public static PullRequest toPullRequest(MergeRequest m, List<Commit> commits, URL url, Repository repo, PullRequestHome prHome) {
        return new PullRequest(m.getIid().toString(),
                url,
                repo,  // repo
                new Codebase(m.getTargetBranch()), // codebase
                toPullRequestState(m.getState()), // state
                m.getTitle(), // title
                m.getDescription(), // body
                toMergeable(m.getMergeStatus()), // mergeable
                m.getMergedAt() != null, // merged
                MergeableState.UNKNOWN, //merge state
                m.getMergedAt(),
                commits.stream().map(commit -> new org.jboss.set.aphrodite.domain.Commit(commit.getId(), commit.getMessage())).collect(Collectors.toList()),
                prHome);
    }

    /**
     * Converts a gitlab label into an aphrodite one.
     *
     * @param l The gitlab label
     * @param repoUrl The label URL
     * @return The aphrodite label
     */
    public static Label toLabel(org.gitlab4j.api.models.Label l, URL repoUrl) {
        return new Label(l.getId().toString(), l.getColor(), l.getName(), repoUrl + "/labels/" + l.getName());
    }
}
