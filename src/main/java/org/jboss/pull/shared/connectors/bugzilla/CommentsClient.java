/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.pull.shared.connectors.bugzilla;

import static org.jboss.pull.shared.internal.XMLRPC.Array;
import static org.jboss.pull.shared.internal.XMLRPC.Struct;
import static org.jboss.pull.shared.internal.XMLRPC.cast;
import static org.jboss.pull.shared.internal.XMLRPC.iterable;

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
        Map<String, ?> results = fetch(Struct, METHOD_BUG_COMMENTS, params);

        if (results != null && !results.isEmpty() && results.containsKey("bugs")) {
            final Map<String, Object> bugs = cast(Struct, results.get("bugs"));
            return buildComments(cast(Struct, bugs.get(Integer.toString(bug.getId()))));
        }
        return new TreeSet<Comment>();
    }

    @SuppressWarnings("unchecked")
    private SortedSet<Comment> buildComments(Map<String, Object> bug) {
        SortedSet<Comment> bugComments = new TreeSet<Comment>();
        for (Object[] comments : iterable(Array, bug.values())) {
            for (Map<String, Object> comment : iterable(Struct, comments)) {
                bugComments.add(new Comment(comment));
            }
        }
        return bugComments;
    }

    @SuppressWarnings("unchecked")
    private SortedSet<Comment> createComments(final Object[] objects) {
        SortedSet<Comment> comments = new TreeSet<Comment>();
        for (Map<String, ?> comment : iterable(Struct, objects)) {
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
        Map<String, ?> results = fetch(Struct, METHOD_BUG_COMMENTS, params);

        Map<String, SortedSet<Comment>> commentsByBugId = new HashMap<String, SortedSet<Comment>>();
        if (results != null && !results.isEmpty() && results.containsKey("bugs"))
            for (Entry<String, Object> bug : cast(Struct, results.get("bugs")).entrySet())
                commentsByBugId.put(bug.getKey(), createComments(cast(Array, cast(Struct, bug.getValue()).get("comments"))));
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
