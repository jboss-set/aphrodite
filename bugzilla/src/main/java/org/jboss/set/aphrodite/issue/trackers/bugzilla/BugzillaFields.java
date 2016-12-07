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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import org.jboss.set.aphrodite.domain.Flag;

import java.util.Optional;

/**
 * @author Ryan Emerson
 */
class BugzillaFields {
    static final String API_URL = "xmlrpc.cgi";
    static final String ID_QUERY = "show_bug.cgi?id=";

    static final String LOGIN = "Bugzilla_login";
    static final String PASSWORD = "Bugzilla_password";

    // Issue fields
    static final String ASSIGNEE = "assigned_to";
    static final String BLOCKS = "blocks";
    static final String COMMENT = "comment";
    static final String COMMENT_ID = "id";
    static final String COMMENT_IS_PRIVATE = "is_private";
    static final String COMMENT_BODY = "text";
    static final String COMMENT_BUG_ID = "bug_id";
    static final String COMPONENT = "component";
    static final String CREATION_TIME = "creation_time";
    static final String DEPENDS_ON = "depends_on";
    static final String EXTERNAL_URL = "external_bugs";
    static final String DESCRIPTION = "description";
    static final String ESTIMATED_TIME = "estimated_time";
    static final String FILTER_SHARER_ID = "sharer_id";
    static final String FLAG_ACK_DEVEL = "devel_ack";
    static final String FLAG_ACK_PM = "pm_ack";
    static final String FLAG_ACK_QA = "qa_ack";
    static final String FLAGS = "flags";
    static final String FLAG_NAME = "name";
    static final String FLAG_STATUS = "status";
    static final String HOURS_WORKED = "actual_time";
    static final String ID = "id";
    static final String ISSUE_IDS = "ids";
    static final String ISSUE_TYPE = "cf_type";
    static final String LAST_UPDATED = "last_change_time";
    static final String NAME = "name";
    static final String PRIVATE_COMMENT = "private";
    static final String PRODUCT = "product";
    static final String REPORTER = "creator";
    static final String SUMMARY = "summary";
    static final String STATUS = "status";
    static final String PRIORITY = "priority";
    static final String TARGET_MILESTONE = "target_milestone";
    static final String TARGET_RELEASE = "target_release";
    static final String UPDATE_FIELDS = "updates";
    static final String VERSION = "version";

    static final String METHOD_GET_BUG = "Bug.get";
    static final String METHOD_UPDATE_BUG = "Bug.update";
    static final String METHOD_ADD_COMMENT = "Bug.add_comment";
    static final String METHOD_GET_COMMENT = "Bug.comments";
    static final String METHOD_SEARCH = "Bug.search";
    static final String METHOD_FILTER_SEARCH = "savedsearch";
    static final String METHOD_USER_LOGIN = "User.login";
    static final String METHOD_SET_COLLECTION = "set";

    static final String RESULT_BUGS = "bugs";
    static final String RESULT_INCLUDE_FIELDS = "include_fields";
    static final String RESULT_LIMIT = "limit";
    static final String RESULT_PERMISSIVE_SEARCH = "permissive";

    static final String SEARCH_EQUALS = "equals";
    static final String SEARCH_FLAGS = "flagtypes.name";
    static final String SEARCH_FUNCTION = "f";
    static final String SEARCH_OPTION = "o";
    static final String SEARCH_VALUE = "v";

    static final Object[] RESULT_FIELDS = { ASSIGNEE, BLOCKS, COMPONENT, CREATION_TIME, LAST_UPDATED, DEPENDS_ON, SUMMARY,
            DESCRIPTION, ESTIMATED_TIME, FLAGS, HOURS_WORKED, ID, ISSUE_TYPE, PRODUCT, REPORTER, STATUS, PRIORITY, TARGET_MILESTONE,
            TARGET_RELEASE, VERSION, EXTERNAL_URL };

    static final Object[] COMMENT_FIELDS = { COMMENT_BUG_ID, COMMENT_ID, COMMENT_BODY, COMMENT_IS_PRIVATE };

    static Optional<Flag> getAphroditeFlag(String bzFlag) {
        switch (bzFlag) {
            case FLAG_ACK_DEVEL:
                return Optional.of(Flag.DEV);
            case FLAG_ACK_PM:
                return Optional.of(Flag.PM);
            case FLAG_ACK_QA:
                return Optional.of(Flag.QE);
        }
        return Optional.empty();
    }

    static Optional<String> getBugzillaFlag(Flag flag) {
        switch (flag) {
            case DEV:
                return Optional.of(FLAG_ACK_DEVEL);
            case PM:
                return Optional.of(FLAG_ACK_PM);
            case QE:
                return Optional.of(FLAG_ACK_QA);
        }
        return Optional.empty();
    }
}
