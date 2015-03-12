package org.jboss.pull.shared.connectors.bugzilla;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.pull.shared.connectors.common.Flag.Status;

public class CommentsClient extends BugsClient {

    private static final String METHOD_FLAG_UPDATE = "Flag.update";
    private static final String METHOD_BUG_COMMENTS = "Bug.comments";
    private static final String METHOD_BUG_ADD_COMMENT = "Bug.add_comment";

    public CommentsClient(String serverUrl, String login, String password) {
        super(serverUrl, login, password);
    }

    /**
     * Update Bugzilla bugs flag status.
     *
     * @param ids An array of integers, a single integer representing one or more bug ids
     * @param name The name of the flag that supposes to be updated
     * @param status The flag's new status(+,-,X,?)
     * @return true if update successful, otherwise false;
     */
    public boolean updateBugzillaFlag(Integer[] ids, String name, Status status) {

        String flagStatus = getFlagStatusFrom(status);

        Map<String, Object> params = getParameterMap();
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("name", name);
        updates.put("status", flagStatus);
        Object[] updateArray = { updates };
        params.put("ids", ids);
        params.put("updates", updateArray);
        params.put("permissive", true);

        return runCommand(METHOD_FLAG_UPDATE, params);
    }

    @SuppressWarnings("unchecked")
    public SortedSet<Comment> commentsFor(Bug bug) {
        if (bug == null)
            throw new IllegalArgumentException("Provided bug instance can't be null.");

        Map<String, Object> params = getParameterMap();
        params.put("ids", new Integer[] { bug.getId() });
        Map<String, ?> results = fetchData(METHOD_BUG_COMMENTS, params);

        if (results != null && !results.isEmpty() && results.containsKey("bugs")) {
            final Map<String, Map<String, Object[]>> bugs = (Map<String, Map<String, Object[]>>) results.get("bugs");
            return buildComments(bugs.get(Integer.toString(bug.getId())));
        }
        return new TreeSet<Comment>();
    }

    @SuppressWarnings("unchecked")
    private SortedSet<Comment> buildComments(Map<String, Object[]> bug) {
        SortedSet<Comment> bugComments = new TreeSet<Comment>();
        for (Object[] comments : bug.values()) {
            for (Object comment : comments) {
                bugComments.add(new Comment((Map<String, ?>) comment));
            }
        }
        return bugComments;
    }

    @SuppressWarnings("unchecked")
    private SortedSet<Comment> createComments(Map<String, ?>[] objects) {
        SortedSet<Comment> comments = new TreeSet<Comment>();
        for (Map<String, ?> comment : objects) {
            comments.add(new Comment(comment));
        }
        return comments;
    }

    @SuppressWarnings("unchecked")
    public Map<String, SortedSet<Comment>> commentsFor(Collection<String> bugIds) {
        if (bugIds == null || bugIds.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<String, Object> params = getParameterMap();
        params.put("ids", bugIds.toArray());
        Map<String, ?> results = fetchData(METHOD_BUG_COMMENTS, params);

        Map<String, SortedSet<Comment>> commentsByBugId = new HashMap<String, SortedSet<Comment>>();
        if (results != null && !results.isEmpty() && results.containsKey("bugs"))
            for (Entry<String, Map<String, Map<String, ?>[]>> bug : ((Map<String, Map<String, Map<String, ?>[]>>) results.get("bugs")).entrySet())
                commentsByBugId.put(bug.getKey(), createComments(bug.getValue().get("comments")));
        return commentsByBugId;
    }

    public boolean addComment(final int id, final String text, final CommentVisibility visibility, final double worktime) {

        Map<String, Object> params = getParameterMap();
        params.put("id", id);
        params.put("comment", text);
        params.put("private", visibility.isPrivate());
        params.put("work_time", worktime);

        return runCommand(METHOD_BUG_ADD_COMMENT, params);
    }

    private String getFlagStatusFrom(Status status) {
        String flagStatus;
        if (status.equals(Status.POSITIVE))
            flagStatus = "+";
        else if (status.equals(Status.NEGATIVE))
            flagStatus = "-";
        else if (status.equals(Status.UNKNOWN))
            flagStatus = "?";
        else
            flagStatus = " ";
        return flagStatus;
    }

    /**
     * Post a new comment on Bugzilla
     *
     * @param bugzillaId Bugzilla identity
     * @param comment The comment will be posted
     * @return true if post successes
     */
    public boolean postBugzillaComment(Integer bugzillaId, String comment) {
        Map<String, Object> params = getParameterMap();
        params.put("id", bugzillaId);
        params.put("comment", comment);

        return runCommand("Bug.update", params);
    }


}
