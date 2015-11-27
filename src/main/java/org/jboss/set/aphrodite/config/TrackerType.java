package org.jboss.set.aphrodite.config;

public enum TrackerType {

    JIRA("jira"), BUGZILLA("bugzilla");

    private final String typename;

    private TrackerType(final String typename) {
        this.typename = typename;
    }

    @Override
    public String toString() {
        return typename;
    }
}
