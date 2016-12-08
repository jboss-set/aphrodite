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
package org.jboss.set.payload.report.jira.rest.client.internal.async;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.internal.async.AbstractAsynchronousRestClient;
import com.atlassian.jira.rest.client.internal.json.JsonObjectParser;
import com.atlassian.util.concurrent.Promise;
import org.jboss.set.payload.report.jira.rest.client.api.domain.Board;
import org.jboss.set.payload.report.jira.rest.client.api.domain.Page;
import org.jboss.set.payload.report.jira.rest.client.api.domain.Sprint;
import org.jboss.set.payload.report.jira.rest.client.internal.json.BoardParser;
import org.jboss.set.payload.report.jira.rest.client.internal.json.BoardsParser;
import org.jboss.set.payload.report.jira.rest.client.internal.json.PageParser;
import org.jboss.set.payload.report.jira.rest.client.internal.json.SprintParser;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AsynchronousAgileRestClient extends AbstractAsynchronousRestClient {
    private final JsonObjectParser<Board> boardParser = new BoardParser();
    private final JsonObjectParser<Page<Board>> boardsParser = new PageParser(boardParser);
    private final JsonObjectParser<Sprint> sprintParser = new SprintParser();
    private final JsonObjectParser<Page<Sprint>> sprintPageParser = new PageParser(sprintParser);

    private final URI baseUri;

    public AsynchronousAgileRestClient(final URI baseUri, final HttpClient client) {
        super(client);
        this.baseUri = baseUri;
    }

    public Promise<Page<Board>> getAllBoards(final Long startAt, final Integer maxResults, @Deprecated final String type, final String name, final String projectKeyOrId) {
        final UriBuilder builder = UriBuilder.fromUri(baseUri).path("board");
        if (startAt != null) builder.queryParam("startAt", startAt);
        if (maxResults != null) builder.queryParam("maxResults", maxResults);
        if (type != null) builder.queryParam("type", type);
        if (name != null) builder.queryParam("name", name);
        if (projectKeyOrId != null) builder.queryParam("projectKeyOrId", projectKeyOrId);
        final URI uri = builder.build();
        return getAndParse(uri, boardsParser);
    }

    // https://docs.atlassian.com/jira-software/REST/latest/#agile/1.0/board/{boardId}/sprint-getAllSprints
    public Promise<Page<Sprint>> getAllSprints(final int boardId, final Long startAt, final Integer maxResults, @Deprecated final String state) {
        final UriBuilder builder = UriBuilder.fromUri(baseUri).path("board").path(Integer.toString(boardId)).path("sprint");
        if (startAt != null) builder.queryParam("startAt", startAt);
        if (maxResults != null) builder.queryParam("maxResults", maxResults);
        if (state != null) builder.queryParam("state", state);
        final URI uri = builder.build();
        return getAndParse(uri, sprintPageParser);
    }
}
