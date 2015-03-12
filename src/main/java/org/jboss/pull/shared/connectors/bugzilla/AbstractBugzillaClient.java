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
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public abstract class AbstractBugzillaClient {

    private String baseURL;
    private String login;
    private String password;


    public AbstractBugzillaClient(String serverUrl, String login, String password) {
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
    protected Map<String, Object> getParameterMap() {
        Map<String, Object> params = new HashMap<String, Object>();
        if (login != null)
            params.put("Bugzilla_login", login);
        if (password != null)
            params.put("Bugzilla_password", password);
        return params;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ?> fetchData(String method, Object... params) {
        try {
            return (Map<String, ?>) getClient().execute(method, params);
        } catch (XmlRpcException e) {
            throw new IllegalStateException(e);
        }
    }

    protected boolean runCommand(String method, Object... params) {
        try {
            getClient().execute(method, params);
            return true;
        } catch (XmlRpcException e) {
            throw new IllegalStateException(e);
        }
    }

    private URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can not get XmlRpcClient from " + baseURL, e);
        }
    }
}
