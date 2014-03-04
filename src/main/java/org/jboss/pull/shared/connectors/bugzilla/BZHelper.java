package org.jboss.pull.shared.connectors.bugzilla;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.Util;

public class BZHelper {

    private static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/";
    public static final Pattern BUGZILLA_ID_PATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);


    private final String BUGZILLA_LOGIN;
    private final String BUGZILLA_PASSWORD;

    private final Bugzilla bugzillaClient;

    public BZHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {
            Properties props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

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

    public List<Bug> getBugFromDescription(PullRequest pull) {
        final List<Integer> ids = checkBugzillaId(pull.getBody());
        final ArrayList<Bug> bugs = new ArrayList<Bug>();

        for (Integer id : ids) {
            final Bug bug = getBug(id);
            if( bug != null ){
                bugs.add(bug);
            }
        }
        return bugs;
    }

    private List<Integer> checkBugzillaId(String body) {
        final ArrayList<Integer> ids = new ArrayList<Integer>();
        final Matcher matcher = BUGZILLA_ID_PATTERN.matcher(body);
        while (matcher.find()) {
            try {
                ids.add(Integer.valueOf(matcher.group(1)));
            } catch (NumberFormatException ignore) {
                System.err.printf("Invalid bug number: %s.\n", ignore);
            }
        }
        return ids;
    }

}
