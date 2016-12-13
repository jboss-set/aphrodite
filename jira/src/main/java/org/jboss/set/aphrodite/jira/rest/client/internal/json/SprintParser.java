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

import com.atlassian.jira.rest.client.internal.json.JsonObjectParser;
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.set.payload.report.jira.rest.client.api.domain.Sprint;
import org.joda.time.DateTime;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SprintParser implements JsonObjectParser<Sprint> {
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Override
    public Sprint parse(final JSONObject json) throws JSONException {
        // https://docs.atlassian.com/jira-software/REST/latest/#agile/1.0/board/{boardId}/sprint
        // {"id":4786,"self":"https://issues.jboss.org/rest/agile/1.0/sprint/4786","state":"closed","name":"EAP 7.0.1","startDate":"2016-05-11T02:48:59.548-04:00","endDate":"2016-07-20T02:48:00.000-04:00","completeDate":"2016-07-26T03:41:18.239-04:00","originBoardId":3466}
        final Long id = JsonParseUtil.getOptionalLong(json, "id");
        final URI self = JsonParseUtil.getSelfUri(json);
        final String state = json.getString("state");
        final String name = json.getString("name");
        final Date startDate = parseDateTime(json.getString("startDate"));
        final Date endDate = parseDateTime(json.getString("endDate"));
        final Date completeDate = parseDateTime(json.optString("completeDate", null));
        final Integer originBoardId = JsonParseUtil.parseOptionInteger(json, "originBoardId");
        return new Sprint(self, id, state, name, startDate, endDate, completeDate, originBoardId);
    }

    private static Date parseDateTime(final String source) throws JSONException {
        if (source == null) return null;
        try {
            return DATE_FORMAT.parse(source);
        } catch (ParseException e) {
            throw new JSONException(e);
        }
    }
}
