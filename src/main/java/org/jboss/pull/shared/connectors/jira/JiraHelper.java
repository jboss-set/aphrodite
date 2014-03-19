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
import com.atlassian.jira.rest.client.ProgressMonitor;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import org.jboss.pull.shared.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author navssurtani
 */
public class JiraHelper {

    private static String JIRA_LOGIN;
    private static String JIRA_PASSWORD;
    private static String JIRA_BASE_URL;

    private JiraRestClient restClient;

    public JiraHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {

        try {
            readJiraCredentials(configurationFileProperty, configurationFileDefault);
            restClient = buildJiraRestClient();
        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }
    }

    /**
     * Get the JIRA issue from the remote server
     *
     * @param issueId - the JIRA ID of the issue we wish to find. For example, WFLY-123 is a valid id.
     * @return - a {@link org.jboss.pull.shared.connectors.jira.JiraIssue} bean.
     */
    public JiraIssue getJIRA(String issueId) {
        ProgressMonitor monitor = new NullProgressMonitor();
        com.atlassian.jira.rest.client.domain.Issue issue = restClient.getIssueClient().getIssue(issueId, monitor);
        return new JiraIssue(issue);
    }

    private void readJiraCredentials(String configurationFileProperty, String configurationFileDefault) throws IOException {
        Properties props = Util.loadProperties(configurationFileProperty, configurationFileDefault);
        JIRA_LOGIN = Util.require(props, "jira.login");
        JIRA_PASSWORD = Util.require(props, "jira.password");
        JIRA_BASE_URL = Util.require(props, "jira.base.url");
    }

    private JiraRestClient buildJiraRestClient() throws URISyntaxException {
        JerseyJiraRestClientFactory clientFactory = new JerseyJiraRestClientFactory();
        return clientFactory.createWithBasicHttpAuthentication(new URI(JIRA_BASE_URL), JIRA_LOGIN, JIRA_PASSWORD);
    }
}
