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
package org.jboss.set.payload.report.jira.rest.client.internal.json;

import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.internal.json.JsonObjectParser;
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.set.payload.report.jira.rest.client.api.domain.Board;

import java.net.URI;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class BoardParser implements JsonObjectParser<Board> {
    // "id":101,"self":"https://issues.jboss.org/rest/agile/1.0/board/101","name":"RichFaces Sandbox","type":"scrum"
    @Override
    public Board parse(final JSONObject json) throws JSONException {
        final Long id = JsonParseUtil.getOptionalLong(json, "id");
        final URI self = JsonParseUtil.getSelfUri(json);
        final String name = json.getString("name");
        final String type = json.getString("type");
        //return new Status(self, id, name, description, iconUrl);
        return new Board(self, id, name, type);
    }
}
