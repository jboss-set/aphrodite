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
package org.jboss.pull.shared;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.jboss.pull.shared.Flag.Status;

public class Bugzilla {

    private String baseURL;
    private String login;
    private String password;

    public Bugzilla(String serverUrl, String login, String password) {
        this.baseURL = serverUrl;
        this.login = login;
        this.password = password;
    }

    /**
     * Get a new XmlRpcClient instance from server URL
     *
     * @param serverURL Bugzilla base URL
     * @return XmlRpcClient
     */
    private XmlRpcClient getClient(String serverURL) {
        try {
            String apiURL = serverURL + "xmlrpc.cgi";
            XmlRpcClient rpcClient;
            XmlRpcClientConfigImpl config;
            config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(apiURL));
            rpcClient = new XmlRpcClient();
            rpcClient.setConfig(config);
            return rpcClient;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bug getBug(Integer bugzillaId) {
        //TODO
        return null;
    }

    /**
     * Post a new comment on Bugzilla
     *
     * @param bugzillaId Bugzilla identity
     * @param comment The comment will be posted
     * @return true if post successes
     */
    public boolean postBugzillaComment(Integer bugzillaId, String comment) {
        System.out.println(comment);
        try {
            XmlRpcClient rpcClient = getClient(baseURL);
            Map<Object, Object> params = new HashMap<Object, Object>();

            params.put("Bugzilla_login", login);
            params.put("Bugzilla_password", password);
            params.put("id", bugzillaId);
            params.put("comment", comment);
            Object[] objs = { params };
            Object result = (HashMap) rpcClient.execute("Bug.add_comment", objs);

            return true;

        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Change Bugzilla bug status.
     *
     * @param bugzillaId id. The ids of the bugs that you want to modify
     * @param status The status you want to change the bug to
     * @return true if status changed otherwise false
     */
    @SuppressWarnings("unchecked")
    public boolean updateBugzillaStatus(Integer bugzillaId, Bug.Status status) {
        try {
            XmlRpcClient rpcClient = getClient(baseURL);
            Map<Object, Object> params = new HashMap<Object, Object>();

            // update bug status.
            params.put("Bugzilla_login", login);
            params.put("Bugzilla_password", password);
            params.put("ids", bugzillaId);
            params.put("status", status);
            Object[] objParams = { params };
            Object result = (HashMap) rpcClient.execute("Bug.update", objParams);
            Map<Object,Object> res = (Map<Object, Object>) result;
            int id = (Integer) res.get("id");
            return id == bugzillaId;

        } catch (XmlRpcException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update Bugzilla bugs flag status.
     *
     * @param ids An array of integers, a single integer representing one or more bug ids
     * @param name The name of the flag that supposes to be updated
     * @param status The flag's new status(+,-,X,?)
     * @return true if update successful, otherwise false;
     */
    @SuppressWarnings("unused")
    private boolean updateBugzillaFlag(Integer[] ids, String name, Status status) {

        String flagStatus;
        if(status.equals(Status.POSITIVE))
            flagStatus = "+";
        else if(status.equals(Status.NEGATIVE))
            flagStatus = "-";
        else if (status.equals(Status.UNKNOWN))
            flagStatus = "?";
        else
            flagStatus = " ";

        XmlRpcClient rpcClient = getClient(baseURL);
        Map<Object, Object> params = new HashMap<Object, Object>();

        HashMap<String, String> updates = new HashMap<String, String>();
        updates.put("name", name);
        updates.put("status", flagStatus);
        Object[] updateArray = {updates};

        //update bugzilla bugs flag.
        params.put("Bugzilla_login", login);
        params.put("Bugzilla_password", password);
        params.put("ids", ids);
        params.put("updates", updateArray);
        params.put("permissive", true);

        Object[] objs = {params};
        try {
            rpcClient.execute("Flag.update", objs);
            return true;
        } catch (XmlRpcException e) {
            e.printStackTrace();
            return false;
        }
    }
}