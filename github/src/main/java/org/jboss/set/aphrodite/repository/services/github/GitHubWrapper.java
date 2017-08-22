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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.MergeableState;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;

/**
 * @author Ryan Emerson
 */
class GitHubWrapper {

    private static final Log LOG = LogFactory.getLog(GitHubWrapper.class);

    Repository toAphroditeRepository(URL url, Collection<GHBranch> branches) {
        Repository repo = new Repository(url);
        List<Codebase> branchNames = branches.stream()
                .map(this::repositoryBranchToCodebase)
                .collect(Collectors.toList());
        repo.getCodebases().addAll(branchNames);
        return repo;
    }

    List<PullRequest> toAphroditePullRequests(List<GHPullRequest> pullRequests) {
        return pullRequests.stream()
                .map(this::pullRequestToPullRequest)
                .collect(Collectors.toList());
    }

    PullRequest pullRequestToPullRequest(GHPullRequest pullRequest) {
        try {
            final String id = Integer.toString(pullRequest.getNumber());
            final URL url = pullRequest.getHtmlUrl();
            final Codebase codebase = new Codebase(pullRequest.getBase().getRef());
            final PullRequestState state = getPullRequestState(pullRequest.getState());
            final String title = pullRequest.getTitle() == null ? "" : pullRequest.getTitle().replaceFirst("\\u2026", "");
            final String body = pullRequest.getBody() == null ? "" : pullRequest.getBody().replaceFirst("\\u2026", "");
            boolean mergeable = false;
            if (pullRequest.getMergeable() == null) {
                // workaround https://github.com/jboss-set/aphrodite/issues/150
                Utils.logWarnMessage(LOG, "Can not retreive " + pullRequest.getHtmlUrl() + " mergeable value");
            } else {
                mergeable = pullRequest.getMergeable();
            }
            final boolean merged = pullRequest.isMerged();
            final Date mergedAt = pullRequest.getMergedAt();
            final MergeableState mergeableState = pullRequest.getMergeableState() == null ? null : MergeableState.valueOf(pullRequest.getMergeableState().toUpperCase());
            String urlString = url.toString();
            int idx = urlString.indexOf("pull");
            if (idx >= 0) {
                urlString = urlString.substring(0, idx);
            }
            final Repository repo = new Repository(URI.create(urlString).toURL());

            return new PullRequest(id, url, repo, codebase, state, title, body, mergeable, merged, mergeableState, mergedAt);
        } catch (IOException e) {
            Utils.logException(LOG, e);
            return null;
        }
    }

    public List<Label> pullRequestLabeltoPullRequestLabel(Collection<GHLabel> labels) {
        List<Label> patchLabels = new ArrayList<>();
        for (GHLabel label : labels) {
            String name = label.getName();
            String color = label.getColor();
            String url = label.getUrl();

            patchLabels.add(new Label(color, name, url));
        }
        return patchLabels;
    }

    public static PullRequestState getPullRequestState(GHIssueState state) {
        try {
            return PullRequestState.valueOf(state.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PullRequestState.UNDEFINED;
        }
    }

    public RateLimit ghRateLimittoRateLimit(GHRateLimit ghRateLimit) {
        return new RateLimit(ghRateLimit.remaining, ghRateLimit.limit, ghRateLimit.reset);
    }

    private Codebase repositoryBranchToCodebase(GHBranch branch) {
        return new Codebase(branch.getName());
    }
}