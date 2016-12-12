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

import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ryan Emerson
 */
class GitHubWrapper {

    Repository toAphroditeRepository(URL url, Collection<GHBranch> branches) {
        Repository repo = new Repository(url);
        List<Codebase> branchNames = branches.stream()
                .map(this::repositoryBranchToCodebase)
                .collect(Collectors.toList());
        repo.getCodebases().addAll(branchNames);
        return repo;
    }

    List<Patch> toAphroditePatches(List<GHPullRequest> pullRequests) {
        return pullRequests.stream()
                .map(this::pullRequestToPatch)
                .collect(Collectors.toList());
    }

    Patch pullRequestToPatch(GHPullRequest pullRequest) {
        try {
            String id = Integer.toString(pullRequest.getNumber());
            URL url = pullRequest.getHtmlUrl();
            Codebase codebase = new Codebase(pullRequest.getBase().getRef());
            PatchState state = getPatchState(pullRequest.getState());
            String title = pullRequest.getTitle().replaceFirst("\\u2026", "");
            String body = pullRequest.getBody().replaceFirst("\\u2026", "");

            String urlString = url.toString();
            int idx = urlString.indexOf("pull");
            if (idx >= 0) {
                urlString = urlString.substring(0, idx);
            }
            Repository repo = new Repository(URI.create(urlString).toURL());

            return new Patch(id, url, repo, codebase, state, title, body);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public List<Label> pullRequestLabeltoPatchLabel(Collection<GHLabel> labels) {
        List<Label> patchLabels = new ArrayList<>();
        for (GHLabel label : labels) {
            String name = label.getName();
            String color = label.getColor();
            String url = label.getUrl();

            patchLabels.add(new Label(color, name, url));
        }
        return patchLabels;
    }

    public static PatchState getPatchState(GHIssueState state) {
        try {
            return PatchState.valueOf(state.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PatchState.UNDEFINED;
        }
    }

    public RateLimit ghRateLimittoRateLimit(GHRateLimit ghRateLimit) {
        return new RateLimit(ghRateLimit.remaining, ghRateLimit.limit, ghRateLimit.reset);
    }

    private Codebase repositoryBranchToCodebase(GHBranch branch) {
        return new Codebase(branch.getName());
    }
}