package org.jboss.pull.shared.connectors.bugzilla;

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

        Map<String, ?> resultMap = fetchData(METHOD_BUG_GET, params);

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

    @SuppressWarnings("unchecked")
    public Map<String, Bug> getBugs(Set<String> keySet) {
        if (keySet == null || keySet.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<String, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", keySet.toArray(new String[keySet.size()]));
        params.put("permissive", true);

        Map<String, Bug> results = new HashMap<String, Bug>(keySet.size());

        Map<String, ?> resultMap = fetchData(METHOD_BUG_GET, params);
        if (resultMap != null && !resultMap.isEmpty()) {
            Map<String, Object>[] bugs = (Map<String, Object>[]) resultMap.get("bugs");
            for (int i = 0; i < bugs.length; i++) {
                Bug bug = new Bug(bugs[i]);
                results.put(Integer.toString(bug.getId()), bug);
            }
        }
        return results;
    }

}