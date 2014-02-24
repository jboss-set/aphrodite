package org.jboss.pull.shared.connectors.bugzilla;

import java.util.Properties;

import org.jboss.pull.shared.Util;

public class BZHelper {

    private static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/";

    private final String BUGZILLA_LOGIN;
    private final String BUGZILLA_PASSWORD;

    private final Bugzilla bugzillaClient;

    private final Properties props;

    public BZHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {

            props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

            BUGZILLA_LOGIN = Util.require(props, "bugzilla.login");
            BUGZILLA_PASSWORD = Util.require(props, "bugzilla.password");

            // initialize bugzilla client
            bugzillaClient = new Bugzilla(BUGZILLA_BASE, BUGZILLA_LOGIN, BUGZILLA_PASSWORD);
        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }
    }
    // -------- Bugzilla related methods
    public Bug getBug(Integer bugzillaId) {
        return bugzillaClient.getBug(bugzillaId);
    }

    public boolean updateBugzillaStatus(Integer bugzillaId, Bug.Status status) {
        return bugzillaClient.updateBugzillaStatus(bugzillaId, status);
    }

    public Properties getProps() {
        return props;
    }

}
