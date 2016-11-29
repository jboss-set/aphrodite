package org.jboss.set.aphrodite.issue.trackers.jira;

public enum JiraIssueResolution {

    DONE(1,"DONE"), REJECTED(2, "REJECTED"), DUPLICATE_ISSUE(3,"DUPLICATE ISSUE"), INCOMPLETE_DESCRIPTION(4,"INCOMPLETE DESCRIPTION"), CANNOT_REPRODUCE_BUG(
            5, "CANNOT REPRODUCE BUG"), PARTIALLY_COMPLETED(7,"PARTIALLY COMPLETED"), DEFERRED(8,"DEFERRED"), WONTFIX(9,"WON'T FIX"), OUT_OF_DATE(10,
            "OUT OF DATE"), MIGRATED(11,"MIGRATED TO ANOTHER ITS"), RESOLVED_AT_APACHE(12, "RESOLVED AT APACHE"), UNRESOLVED(
            0,"UNRESOLVED"), WONTDO(10000, "WON'T DO"), CANNOT_REPRODUCE(10002, "CANNOT REPRODUCE"), DUPLICATE(10200,"DUPLICATE");

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