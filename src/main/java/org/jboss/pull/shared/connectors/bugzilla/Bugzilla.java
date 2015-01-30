/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.jboss.pull.shared.connectors.common.Flag.Status;

public class Bugzilla {

    private String baseURL;
    private String login;
    private String password;

    private static final String METHOD_BUG_UPDATE = "Bug.update";
    private static final String METHOD_FLAG_UPDATE = "Flag.update";
    private static final String METHOD_BUG_GET = "Bug.get";
    private static final String METHOD_BUG_COMMENTS = "Bug.comments";

    public Bugzilla(String serverUrl, String login, String password) {
        this.baseURL = serverUrl;
        this.login = login;
        this.password = password;
    }

    /**
     * Get a new XmlRpcClient instance from server URL
     *
     * @return XmlRpcClient
     */
    private XmlRpcClient getClient() {
        String apiURL = baseURL + "xmlrpc.cgi";
        XmlRpcClient rpcClient;
        rpcClient = new XmlRpcClient();
        rpcClient.setConfig(getClientConfig(createURL(apiURL)));
        return rpcClient;
    }

    private XmlRpcClientConfig getClientConfig(URL apiURL) {
        XmlRpcClientConfigImpl config;
        config = new XmlRpcClientConfigImpl();
        config.setServerURL(apiURL);
        return config;
    }


    /**
     * Get an initialized parameter map with login and password
     *
     * @return
     */
    private Map<Object, Object> getParameterMap() {
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("Bugzilla_login", login);
        params.put("Bugzilla_password", password);
        return params;
    }

    private URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can not get XmlRpcClient from " + baseURL, e);
        }
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

    /**
     * Post a new comment on Bugzilla
     *
     * @param bugzillaId Bugzilla identity
     * @param comment The comment will be posted
     * @return true if post successes
     */
    public boolean postBugzillaComment(Integer bugzillaId, String comment) {
        Map<Object, Object> params = getParameterMap();
        params.put("id", bugzillaId);
        params.put("comment", comment);
        Object[] objs = { params };

        return runCommand("Bug.update", objs);
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

    public boolean updateBugzillaTargetRelease(final int ids, final String... targetRelease) {
        Map<Object, Object> params = getParameterMap();

        params.put("ids", ids);
        params.put("target_release", targetRelease);
        Object[] objParams = { params };

        return runCommand(METHOD_BUG_UPDATE, objParams);
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
     * Update Bugzilla bugs flag status.
     *
     * @param ids An array of integers, a single integer representing one or more bug ids
     * @param name The name of the flag that supposes to be updated
     * @param status The flag's new status(+,-,X,?)
     * @return true if update successful, otherwise false;
     */
    public boolean updateBugzillaFlag(Integer[] ids, String name, Status status) {

        String flagStatus = getFlagStatusFrom(status);

        Map<Object, Object> params = getParameterMap();
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("name", name);
        updates.put("status", flagStatus);
        Object[] updateArray = { updates };
        params.put("ids", ids);
        params.put("updates", updateArray);
        params.put("permissive", true);
        Object[] objs = { params };

        return runCommand(METHOD_FLAG_UPDATE, objs);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> fetchData(String method, Object[] params) {
        try {
            return (Map<Object, Object>) getClient().execute(method, params);
        } catch (XmlRpcException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean runCommand(String method, Object[] params) {
        try {
            getClient().execute(method, params);
            return true;
        } catch (XmlRpcException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object[] turnMapIntoObjectArray(Map<Object, Object> params) {
        Object[] objs = { params };
        return objs;
    }

    private Integer[] turnIdIntoAnArray(Integer id) {
        Integer[] ids = new Integer[1];
        ids[0] = id;
        return ids;
    }

    @SuppressWarnings("unchecked")
    public SortedSet<Comment> commentsFor(Bug bug) {
        if (bug == null)
            throw new IllegalArgumentException("Provided bug instance can't be null.");

        Map<Object, Object> params = getParameterMap();
        params.put("ids", turnIdIntoAnArray(bug.getId()));
        Map<Object, Object> results = fetchData(METHOD_BUG_COMMENTS, turnMapIntoObjectArray(params));

        if (results != null && !results.isEmpty() && results.containsKey("bugs")) {
            Map<String, Object> bugs = (Map<String, Object>) results.get("bugs");
            return buildComments((Map<String, Object[]>) bugs.get(String.valueOf(bug.getId())));
        }
        return new TreeSet<Comment>();
    }


    @SuppressWarnings("unchecked")
    private SortedSet<Comment> buildComments(Map<String, Object[]> comments) {
        SortedSet<Comment> bugComments = new TreeSet<Comment>();
        for (Object[] allComments : comments.values()) {
            for (Object comment : allComments) {
                bugComments.add(new Comment((Map<String, Object>) comment));
            }
        }
        return bugComments;
    }

    @SuppressWarnings("unchecked")
    public Map<String, SortedSet<Comment>> commentsFor(Collection<String> bugIds) {
        if (bugIds == null || bugIds.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<Object, Object> params = getParameterMap();
        params.put("ids", bugIds.toArray());
        Map<Object, Object> results = fetchData(METHOD_BUG_COMMENTS, turnMapIntoObjectArray(params));

        Map<String, SortedSet<Comment>> commentsByBugId = new HashMap<String, SortedSet<Comment>>();
        if (results != null && !results.isEmpty() && results.containsKey("bugs"))
            for (Entry<String, Map<String, Object[]>> bug : ((Map<String, Map<String, Object[]>>) results.get("bugs"))
                    .entrySet())
                commentsByBugId.put(bug.getKey(), createComments(bug.getValue().get("comments")));
        return commentsByBugId;
    }

    @SuppressWarnings("unchecked")
    private SortedSet<Comment> createComments(Object[] objects) {
        SortedSet<Comment> comments = new TreeSet<Comment>();
        for (Object comment : objects) {
            comments.add(new Comment((Map<String, Object>) comment));
        }
        return comments;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Bug> getBugs(Set<String> keySet) {
        if (keySet == null || keySet.isEmpty())
            throw new IllegalArgumentException("Provided bug instance can't be null or empty");

        Map<Object, Object> params = getParameterMap();
        params.put("include_fields", Bug.include_fields);
        params.put("ids", turnToStringArray(keySet));
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

    private String[] turnToStringArray(Set<String> set) {
        String[] strings = new String[set.size()];
        int i = 0;
        for (String string : set) {
            strings[i++] = string;
        }
        return strings;
    }
}
