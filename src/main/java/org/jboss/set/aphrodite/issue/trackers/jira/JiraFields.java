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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.spi.AphroditeException;

/**
 * @author Ryan Emerson
 */
class JiraFields {

    static final String API_BASE_PATH = "/rest/api/2/";
    static final String API_FILTER_PATH = API_BASE_PATH + "filter/";
    static final String API_AUTHENTICATION_PATH = API_BASE_PATH + "myself";
    static final String API_ISSUE_PATH = API_BASE_PATH + "issue/";
    static final String BROWSE_ISSUE_PATH = "/browse/";

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

    static final BiMap<String, IssueStatus> STATUS_MAP = new ImmutableBiMap.Builder<String, IssueStatus>()
            .put("open", IssueStatus.NEW)
            .put("coding in progress", IssueStatus.ASSIGNED)
            .put("pull request sent", IssueStatus.POST)
            .put("resolved", IssueStatus.MODIFIED)
            .put("ready for qa", IssueStatus.ON_QA)
            .put("verified", IssueStatus.VERIFIED)
            .put("closed", IssueStatus.CLOSED)
            .build();

    static final BiMap<Flag, String> FLAG_MAP = new ImmutableBiMap.Builder<Flag, String>()
            .put(Flag.DEV, DEV_ACK)
            .put(Flag.PM, PM_ACK)
            .put(Flag.QE, QE_ACK)
            .build();

    static IssueStatus getAphroditeStatus(String status) {
        status = status.toLowerCase();
        IssueStatus issueStatus = STATUS_MAP.get(status);
        if (issueStatus == null) {
            switch (status) {
                case "reopened":
                    return IssueStatus.NEW;
                case "qa in progress":
                    return IssueStatus.ON_QA;
                default:
                    return IssueStatus.UNDEFINED;
            }
        }
        return issueStatus;
    }

    static String getJiraStatus(IssueStatus status) {
        return STATUS_MAP.inverse().get(status);
    }

    static String getJQLField(String field) {
        return String.format(JQL_CUSTOM_TEMPLATE, field);
    }

    static boolean hasSameIssueStatus(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        IssueStatus status = issue.getStatus();
        IssueStatus jiraStatus = getAphroditeStatus(jiraIssue.getStatus().getName());
        return status == jiraStatus;
    }

    static String getJiraTransition(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) throws AphroditeException {
        IssueStatus currentStatus = getAphroditeStatus(jiraIssue.getStatus().getName());
        return getJiraTransition(currentStatus, issue.getStatus());
    }

    // Surely there is a better way of achieving this?????
    static String getJiraTransition(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (currentStatus) {
            case NEW:
                return transitionsForNewStatus(currentStatus, newStatus);
            case ASSIGNED:
                return transitionsForAssignedStatus(currentStatus, newStatus);
            case POST:
                return transitionsForPostStatus(currentStatus, newStatus);
            case MODIFIED:
                return transitionsForModifiedStatus(currentStatus, newStatus);
            case ON_QA:
                return transitionsForON_QAStatus(currentStatus, newStatus);
            case VERIFIED:
                return transitionsForVerifiedStatus(currentStatus, newStatus);
            case CLOSED:
                return transitionsForClosedStatus(currentStatus, newStatus);
            default:
                throw new AphroditeException("It's not possible to transition from " + currentStatus);
        }
    }

    private static String transitionsForNewStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case ASSIGNED:
                return "Start Progress";
            case POST:
                return "Link Pull Request";
            case MODIFIED:
                return "Resolve Issue";
            case CLOSED:
                return "Close Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }

    private static String transitionsForAssignedStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case NEW:
                return "Stop Progress";
            default:
                return transitionsForNewStatus(currentStatus, newStatus);
        }
    }

    private static String transitionsForPostStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case MODIFIED:
                return "Resolve Issue";
            case CLOSED:
                return "Close Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }

    private static String transitionsForModifiedStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case NEW:
            case ASSIGNED:
                return "Reopen Issue";
            case ON_QA:
                return "Hand Over to QA";
            case CLOSED:
                return "Close Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }

    private static String transitionsForON_QAStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case NEW:
            case ASSIGNED:
                return "Reopen Issue";
            case VERIFIED:
                return "Verify Issue";
            case CLOSED:
                return "Close Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }

    private static String transitionsForVerifiedStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case NEW:
            case ASSIGNED:
                return "Reopen Issue";
            case ON_QA:
                return "Retest";
            case CLOSED:
                return "Close Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }

    private static String transitionsForClosedStatus(IssueStatus currentStatus, IssueStatus newStatus) throws AphroditeException {
        switch (newStatus) {
            case NEW:
            case ASSIGNED:
                return "Reopen Issue";
            default:
                throw new AphroditeException("It's not possible to transition from "
                        + currentStatus + " to " + newStatus);
        }
    }
}
