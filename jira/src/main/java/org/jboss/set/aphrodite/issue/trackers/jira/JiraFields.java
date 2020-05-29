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

package org.jboss.set.aphrodite.issue.trackers.jira;

import java.util.regex.Pattern;

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssuePriority;
import org.jboss.set.aphrodite.spi.AphroditeException;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

/**
 * @author Ryan Emerson
 */
class JiraFields {

    static final String API_BASE_PATH = "/rest/api/2/";
    static final String API_ISSUE_PATH = API_BASE_PATH + "issue/";
    static final String BROWSE_ISSUE_PATH = "/browse/";
    static final Pattern PROJECTS_ISSUE_PATTERN = Pattern.compile("\\/projects\\/[^\\/]+\\/issues\\/");

    static final String DATE_STRING_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    static final String JSON_CUSTOM_FIELD = "customfield_";
    static final String JQL_CUSTOM_TEMPLATE = "cf[%s]";

    static final String PM_ACK = "12311242";
    static final String DEV_ACK = "12311243";
    static final String QE_ACK = "12311244";
    static final String TARGET_RELEASE = "12311240";

    static final BiMap<String, String> CUSTOM_FIELD_MAP = new ImmutableBiMap.Builder<String, String>()
            .put(Flag.DEV.toString(), getJQLField(DEV_ACK))
            .put(Flag.PM.toString(), getJQLField(PM_ACK))
            .put(Flag.QE.toString(), getJQLField(QE_ACK))
            .put("TARGET_RELEASE", getJQLField(TARGET_RELEASE))
            .build();

    static final BiMap<String, IssuePriority> PRIORITY_MAP = initPriorityMap();

    static BiMap<String, IssuePriority> initPriorityMap() {
        ImmutableBiMap.Builder<String, IssuePriority> mapBuilder = new ImmutableBiMap.Builder<String, IssuePriority>();
        for (IssuePriority priority : IssuePriority.values())
            mapBuilder.put(priority.toString().toLowerCase(), priority);
        return mapBuilder.build();
    }

    static final BiMap<Flag, String> FLAG_MAP = new ImmutableBiMap.Builder<Flag, String>()
            .put(Flag.DEV, DEV_ACK)
            .put(Flag.PM, PM_ACK)
            .put(Flag.QE, QE_ACK)
            .build();

    static IssuePriority getAphroditePriority(String priority) {
        IssuePriority issueStatus = PRIORITY_MAP.get(priority.toLowerCase());
        return (issueStatus == null ? IssuePriority.UNDEFINED : issueStatus);
    }

    static String getJiraStatus(String status) {
        return status.toUpperCase();
    }

    static String getJQLField(String field) {
        return String.format(JQL_CUSTOM_TEMPLATE, field);
    }

    static boolean hasSameIssueStatus(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        return issue.getStatus().equalsIgnoreCase(jiraIssue.getStatus().getName());
    }

    static String getJiraTransition(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) throws AphroditeException {
        return JiraTransitions.getTransition(jiraIssue.getStatus().getName(), issue.getStatus());
    }
}
