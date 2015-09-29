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

import org.jboss.set.aphrodite.domain.IssueStatus;

/**
 * @author Ryan Emerson
 */
class JiraFields {

    static final String API_BASE_PATH = "/rest/api/2/";
    static final String API_ISSUE_PATH = API_BASE_PATH + "issue/";
    static final String BROWSE_ISSUE_PATH = "/browse/";

    static final String PM_ACK = "customfield_12311242";
    static final String DEV_ACK = "customfield_12311243";
    static final String QE_ACK = "customfield_12311244";
    static final String TARGET_RELEASE = "customfield_12311240";

    static IssueStatus getAphroditeStatus(String status) {
        switch (status.toLowerCase()) {
            case "open":
                return IssueStatus.NEW;
            case "reopened":
            case "coding in progress":
                return IssueStatus.ASSIGNED;
            case "pull request sent":
                return IssueStatus.POST;
            case "resolved":
                return IssueStatus.MODIFIED;
            case "ready for qa":
            case "qa in progress":
                return IssueStatus.ON_QA;
            case "verified":
                return IssueStatus.VERIFIED;
            case "closed":
                return IssueStatus.CLOSED;
            default:
                return IssueStatus.UNDEFINED;
        }
    }
}
