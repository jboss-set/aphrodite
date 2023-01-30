/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.issue.trackers.jira;

public enum JiraIssueResolution {

    DONE(1,"DONE"),
    REJECTED(2, "REJECTED"),
    DUPLICATE_ISSUE(3,"DUPLICATE ISSUE"),
    INCOMPLETE_DESCRIPTION(4,"INCOMPLETE DESCRIPTION"),
    CANNOT_REPRODUCE_BUG(5, "CANNOT REPRODUCE BUG"),
    PARTIALLY_COMPLETED(7,"PARTIALLY COMPLETED"),
    DEFERRED(8,"DEFERRED"),
    WONTFIX(9,"WON'T FIX"),
    OUT_OF_DATE(10,"OUT OF DATE"),
    MIGRATED(11,"MIGRATED TO ANOTHER ITS"),
    RESOLVED_AT_APACHE(12, "RESOLVED AT APACHE"),
    UNRESOLVED(0,"UNRESOLVED"),
    WONTDO(10000, "WON'T DO"),
    CANNOT_REPRODUCE(10002, "CANNOT REPRODUCE"),
    EXPLAINED(10300, "EXPLAINED"),
    DUPLICATE(10700, "DUPLICATE"),
    INCOMPLETE(10800, "INCOMPLETE"),
    FIXED(10801, "FIXED"),
    NOT_A_BUG(10802, "NOT A BUG"),
    ERRATA(10803, "ERRATA"),
    BUGZILLAORPHAN(10804, "BUGZILLAORPHAN"),
    NEXT_RELEASE(10805, "NEXT RELEASE"),
    UPSTREAM(10806, "UPSTREAM"),
    RAWHIDE(10807, "RAWHIDE"),
    CURRENT_RELEASE(10808, "CURRENT RELEASE"),
    EOL(10809, "EOL"),
    OBSOLETE(10900, "OBSOLETE");

    private long id;
    private String label;

    private JiraIssueResolution(final long id, final String label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public long getId() {
        return id;
    }

    public static boolean hasId(long id ) {
        for (JiraIssueResolution resolution: JiraIssueResolution.values() ) {
            if ( resolution.getId() == id )
                return true;
        }
        return false;
    }

    public static JiraIssueResolution getById(long id) {
        for (JiraIssueResolution resolution : JiraIssueResolution.values())
            if (resolution.getId() == id)
                return resolution;

        throw new IllegalArgumentException("No resolution associated with id:" + id);
    }

}