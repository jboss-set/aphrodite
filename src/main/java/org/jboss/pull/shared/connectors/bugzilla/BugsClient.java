package org.jboss.pull.shared.connectors.bugzilla;

import static org.jboss.pull.shared.internal.XMLRPC.Array;
import static org.jboss.pull.shared.internal.XMLRPC.Struct;
import static org.jboss.pull.shared.internal.XMLRPC.cast;
import static org.jboss.pull.shared.internal.XMLRPC.iterable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BugsClient extends AbstractBugzillaClient {

    private static final String METHOD_BUG_UPDATE = "Bug.update";
    private static final String METHOD_BUG_GET = "Bug.get";

    public BugsClient(String serverUrl, String login, String password) {
        super(serverUrl, login, password);
    }

    /**
     * Gets the bugId from bugzilla.
     *
     * @param bugzillaId
     * @return - Bug retrieved from Bugzilla, or null if no bug was found.
     */
    public Bug getBug(int bugzillaId) {
        Map<String, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", bugzillaId);
        params.put("permissive", true);

        Map<String, ?> resultMap = fetch(Struct, METHOD_BUG_GET, params);

        Object[] bugs = (Object[]) resultMap.get("bugs");
        if (bugs.length == 1) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bugMap = (Map<String, Object>) bugs[0];
            Bug bug = new Bug(bugMap);
            return bug;
        } else {
            System.out.println("Zero or more than one bug found with id: " + bugzillaId);
            return null;
        }
    }

    public boolean updateBugzillaTargetRelease(final int ids, final String... targetRelease) {
        Map<String, Object> params = getParameterMap();

        params.put("ids", ids);
        params.put("target_release", targetRelease);

        return runCommand(METHOD_BUG_UPDATE, params);
    }

    /**
     * Change Bugzilla bug status.
     *
     * @param bugzillaId id. The ids of the bugs that you want to modify
     * @param status The status you want to change the bug to
     * @return true if status changed otherwise false
     */
    public boolean updateBugzillaStatus(Integer bugzillaId, Bug.Status status) {
        Map<String, Object> params = getParameterMap();

        params.put("ids", bugzillaId);
        params.put("status", status);

        return runCommand(METHOD_BUG_UPDATE, params);
    }

    public boolean updateBugzillaTargetMilestone(final int ids, final String taregtMilestone) {
        Map<String, Object> params = getParameterMap();

        params.put("ids", ids);
        params.put("target_milestone", taregtMilestone);

        return runCommand(METHOD_BUG_UPDATE, params);
    }


    public boolean updateEstimate(int id, double worktime) {
        Map<String, Object> params = getParameterMap();
        params.put("ids", id);
        params.put("estimated_time", worktime);

        return runCommand("Bug.update", params);
    }


    @SuppressWarnings("unchecked")
    public Map<String, Bug> getBugs(Set<String> keySet) {
        if (keySet == null || keySet.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<String, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", keySet.toArray(new String[keySet.size()]));
        params.put("permissive", true);

        Map<String, Bug> results = new HashMap<String, Bug>(keySet.size());

        Map<String, ?> resultMap = fetch(Struct, METHOD_BUG_GET, params);
        if (resultMap != null && !resultMap.isEmpty()) {
            final Object[] bugs = cast(Array, resultMap.get("bugs"));
            for (Map<String, Object> struct : iterable(Struct, bugs)) {
                Bug bug = new Bug(struct);
                results.put(Integer.toString(bug.getId()), bug);
            }
        }
        return results;
    }

}
