/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import com.google.common.collect.ImmutableMap;
import org.jboss.set.aphrodite.spi.AphroditeException;

import static org.jboss.set.aphrodite.domain.IssueStatus.*;

import java.util.Map;

public class JiraTransitions {
    public static final String BACK_TO_NEW = "Back To New";
    public static final String CLOSE_ISSUE = "Close Issue";
    public static final String DEVEL_APPROVE = "Devel Approve";
    public static final String HAND_OVER_TO_DEV = "Hand Over to Development";
    public static final String HAND_OVER_TO_QA = "Hand Over to QA";
    public static final String LINK_PULL_REQUEST = "Link Pull Request";
    public static final String REJECT_PULL_REQUEST = "Reject Pull Request";
    public static final String REOPEN_ISSUE = "Reopen Issue";
    public static final String REOPEN_ISSUE_FROM_QA = "Reopen Issue From QA";
    public static final String RESOLVE_ISSUE = "Resolve Issue";
    public static final String RETEST = "Retest";
    public static final String START_PROGRESS = "Start Progress";
    public static final String START_QA = "Start QA";
    public static final String STOP_PROGRESS = "Stop Progress";
    public static final String STOP_QA = "Stop QA";
    public static final String UPDATE_PULL_REQUEST = "Update Pull Request";
    public static final String VERIFY_ISSUE = "Verify Issue";


    static final Map<String, Map<String, String>> TRANSITION_TABLE = new ImmutableMap.Builder<String, Map<String, String>>()
            .put(NEW, new ImmutableMap.Builder<String, String>()
                    .put(NEW, DEVEL_APPROVE)
                    .put(OPEN, HAND_OVER_TO_DEV)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(OPEN, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(CODING_IN_PROGRESS, START_PROGRESS)
                    .put(PULL_REQUEST_SENT, LINK_PULL_REQUEST)
                    .put(RESOLVED, RESOLVE_ISSUE)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(CODING_IN_PROGRESS, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(OPEN, STOP_PROGRESS)
                    .put(RESOLVED, RESOLVE_ISSUE)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(PULL_REQUEST_SENT, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(RESOLVED, RESOLVE_ISSUE)
                    .put(CODING_IN_PROGRESS, START_PROGRESS)
                    .put(PULL_REQUEST_SENT, UPDATE_PULL_REQUEST)
                    .put(OPEN, REJECT_PULL_REQUEST)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(RESOLVED, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(REOPENED, REOPEN_ISSUE)
                    .put(READY_FOR_QA, HAND_OVER_TO_QA)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(READY_FOR_QA, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(REOPENED, REOPEN_ISSUE_FROM_QA)
                    .put(QA_IN_PROGRESS, START_QA)
                    .put(VERIFIED, VERIFY_ISSUE)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(QA_IN_PROGRESS, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(REOPENED, REOPEN_ISSUE_FROM_QA)
                    .put(READY_FOR_QA, STOP_QA)
                    .put(VERIFIED, VERIFY_ISSUE)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(VERIFIED, new ImmutableMap.Builder<String, String>()
                    .put(NEW, BACK_TO_NEW)
                    .put(REOPENED, REOPEN_ISSUE)
                    .put(READY_FOR_QA, RETEST)
                    .put(CLOSED, CLOSE_ISSUE)
                    .build())
            .put(CLOSED, new ImmutableMap.Builder<String, String>()
                    .put(REOPENED, REOPEN_ISSUE)
                    .build())
            .build();

    static String getTransition(String currentStatus, String newStatus) throws AphroditeException {
        String currentS = currentStatus.toUpperCase();
        String newS = newStatus.toUpperCase();

        Map<String, String> available = TRANSITION_TABLE.get(currentS);
        if (available != null) {
            String transition = available.get(newS);
            if (transition != null) {
                return transition;
            }
        }

        throw new AphroditeException("It's not possible to transition from " + currentStatus + " to " + newStatus);
    }
}
