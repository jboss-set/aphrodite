/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.pull.shared.connectors.jira;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.IssueHelper;
import org.jboss.pull.shared.connectors.common.Issue;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * @author navssurtani
 */
public class JiraHelper implements IssueHelper {

    private static String JIRA_LOGIN;
    private static String JIRA_PASSWORD;

    private final JiraRestClient restClient;
    private final Properties properties;

    public JiraHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        try {
            properties = Util.loadProperties(configurationFileProperty, configurationFileDefault);
            JIRA_LOGIN = Util.require(properties, "jira.login");
            JIRA_PASSWORD = Util.require(properties, "jira.password");
            restClient = buildJiraRestClient();
        } catch (Exception e) {
            throw Util.logErrorAndGetException(System.err, e);
        }
    }

    @Override
    public Issue findIssue(URL url) throws IllegalArgumentException {
        try {
            String key = cutKeyFromURL(url);
            com.atlassian.jira.rest.client.domain.Issue fromServer = restClient.getIssueClient()
                    .getIssue(key, new NullProgressMonitor());
            return new JiraIssue(fromServer);
        } catch (RuntimeException e) {
            // Atlassian is very poor in reporting proper context
            throw new RuntimeException("Failed to find issue " + url, e);
        }
    }

    @Override
    public boolean accepts(URL url) {
        return url.getHost().equalsIgnoreCase(Constants.JIRA_HOST);
    }

    @Override
    // FIXME: This has to be implemented properly.
    public boolean updateStatus(URL url, Enum status) {
        throw new UnsupportedOperationException("This feature is not supported or tested yet.");
    }

    private JiraRestClient buildJiraRestClient() throws URISyntaxException {
        JerseyJiraRestClientFactory clientFactory = new JerseyJiraRestClientFactory();
        return clientFactory.createWithBasicHttpAuthentication(new URI(Constants.JIRA_BASE), JIRA_LOGIN, JIRA_PASSWORD);
    }

    private String cutKeyFromURL(URL url) {
        String urlString = url.toString();
        int browse = urlString.indexOf("browse/");
        int slashAfterBrowse = urlString.indexOf("/", browse);
        return urlString.substring(slashAfterBrowse + 1);
    }
}
