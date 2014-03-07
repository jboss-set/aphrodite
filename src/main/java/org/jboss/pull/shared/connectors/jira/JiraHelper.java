package org.jboss.pull.shared.connectors.jira;

public class JiraHelper {

    public JiraHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {

        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }
    }

    public JiraIssue getJIRA() {
        return null;
    }
}
