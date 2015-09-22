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
    static final String COMPONENT = "component";
    static final String DEPENDS_ON = "depends_on";
    static final String DESCRIPTION = "description";
    static final String ESTIMATED_TIME = "estimated_time";
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
    static final String NAME = "name";
    static final String PRIVATE_COMMENT = "private";
    static final String PRODUCT = "product";
    static final String STATUS = "status";
    static final String TARGET_MILESTONE = "target_milestone";
    static final String TARGET_RELEASE = "target_release";
    static final String UPDATE_FIELDS = "updates";
    static final String VERSION = "version";

    static final String METHOD_GET_BUG = "Bug.get";
    static final String METHOD_UPDATE_BUG = "Bug.update";
    static final String METHOD_ADD_COMMENT = "Bug.add_comment";
    static final String METHOD_GET_COMMENT = "Bug.comments";
    static final String METHOD_USER_LOGIN = "User.login";

    static final String RESULT_BUGS = "bugs";
    static final String RESULT_INCLUDE_FIELDS = "include_fields";
    static final String RESULT_PERMISSIVE_SEARCH = "permissive";

    static final Object[] RESULT_FIELDS = { ASSIGNEE, BLOCKS, COMPONENT, DEPENDS_ON, DESCRIPTION,
                                            ESTIMATED_TIME, FLAGS, HOURS_WORKED, ID, ISSUE_TYPE,
                                            PRODUCT, STATUS, TARGET_MILESTONE, TARGET_RELEASE,
                                            VERSION };

    static final Object[] COMMENT_FIELDS = {COMMENT_ID, COMMENT_BODY, COMMENT_IS_PRIVATE};

    static Optional<Flag> getFlag(String bzFlag) {
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
}
