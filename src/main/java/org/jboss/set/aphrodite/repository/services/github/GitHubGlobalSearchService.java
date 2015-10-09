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

package org.jboss.set.aphrodite.repository.services.github;

import org.eclipse.egit.github.core.IResourceProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.GitHubService;

import java.io.IOException;
import java.util.List;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_ISSUES;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SEARCH;

/**
 * @author Ryan Emerson
 */
public class GitHubGlobalSearchService extends GitHubService {
    public GitHubGlobalSearchService(GitHubClient client) {
        super(client);
    }

    public List<SearchResult> searchAllPullRequests(String searchString) throws IOException {
        String query = "?q=" + searchString + "+type:pr";
        return searchAllIssues(query);
    }

    public List<SearchResult> searchAllIssues(String query) throws IOException {
        StringBuilder uri = new StringBuilder(SEGMENT_SEARCH + SEGMENT_ISSUES);
        uri.append(query);

        PagedRequest<SearchResult> request = createPagedRequest();
        request.setUri(uri);
        request.setType(IssueContainer.class);
        return getAll(request);
    }

    static class IssueContainer implements IResourceProvider<SearchResult> {
        private List<SearchResult> items;

        @Override
        public List<SearchResult> getResources() {
            return items;
        }

        @Override
        public String toString() {
            return "IssueContainer{" +
                    "items=" + items +
                    '}';
        }
    }
}