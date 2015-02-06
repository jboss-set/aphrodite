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
        Map<Object, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", bugzillaId);
        params.put("permissive", true);
        Object[] objs = { params };

        Map<Object, Object> resultMap = fetchData(METHOD_BUG_GET, objs);

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
        Map<Object, Object> params = getParameterMap();

        params.put("ids", ids);
        params.put("target_release", targetRelease);
        Object[] objParams = { params };

        return runCommand(METHOD_BUG_UPDATE, objParams);
    }

    /**
     * Change Bugzilla bug status.
     *
     * @param bugzillaId id. The ids of the bugs that you want to modify
     * @param status The status you want to change the bug to
     * @return true if status changed otherwise false
     */
    public boolean updateBugzillaStatus(Integer bugzillaId, Bug.Status status) {
        Map<Object, Object> params = getParameterMap();

        params.put("ids", bugzillaId);
        params.put("status", status);
        Object[] objParams = { params };

        return runCommand(METHOD_BUG_UPDATE, objParams);
    }

    public boolean updateBugzillaTargetMilestone(final int ids, final String taregtMilestone) {
        Map<Object, Object> params = getParameterMap();

        params.put("ids", ids);
        params.put("target_milestone", taregtMilestone);
        Object[] objParams = { params };

        return runCommand(METHOD_BUG_UPDATE, objParams);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Bug> getBugs(Set<String> keySet) {
        if (keySet == null || keySet.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<Object, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", ArrayUtils.turnToStringArray(keySet));
        params.put("permissive", true);
        Object[] objs = { params };

        Map<String, Bug> results = new HashMap<String, Bug>(keySet.size());

        Map<Object, Object> resultMap = fetchData(METHOD_BUG_GET, objs);
        if (resultMap != null && !resultMap.isEmpty()) {
            Object[] bugs = (Object[]) resultMap.get("bugs");
            for (int i = 0; i < bugs.length; i++) {
                Bug bug = new Bug((Map<String, Object>) bugs[i]);
                results.put(String.valueOf(bug.getId()), bug);
            }
        }
        return results;
    }

}