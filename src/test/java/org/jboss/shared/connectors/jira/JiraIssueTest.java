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

package org.jboss.shared.connectors.jira;

import org.jboss.pull.shared.connectors.IssueHelper;
import org.jboss.pull.shared.connectors.common.Flag;
import org.jboss.pull.shared.connectors.jira.JiraHelper;
import org.jboss.pull.shared.connectors.jira.JiraIssue;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * @author navssurtani
 *
 * Simple class to setup and test the methods for the {@link org.jboss.pull.shared.connectors.jira.JiraHelper}
 */
@Test(groups="connection")
public class JiraIssueTest {

    private static final String BASE_FILE_PROPERTY = "configuration.base.file";
    private static final String BASE_FILE_NAME = "./processor-eap-6.properties.example";
    private static final String JIRA_URL_BASE = "https://issues.jboss.org/browse/";

    private IssueHelper jiraHelper = null;


    @BeforeTest
    public void startHelper() throws Exception {
        // Because the Util class looks for system properties.
        System.setProperty(BASE_FILE_PROPERTY, BASE_FILE_NAME);
        this.jiraHelper = new JiraHelper(BASE_FILE_PROPERTY, BASE_FILE_NAME);
    }

    @AfterTest
    public void destroyHelper() throws Exception {
        this.jiraHelper = null;
    }

    public void testGetNumber() throws Exception {
        // Basic test method to build a Jira issue, and then validate our return from the server.
        String issueNumber = "EAP6-31";
        URL issueURL = new URL(JIRA_URL_BASE + issueNumber);
        JiraIssue issue = (JiraIssue) jiraHelper.findIssue(issueURL);
        assertEquals(issueNumber, issue.getNumber());
    }

    public void testGetStatus() throws Exception {
        String issueNumber = "WFLY-10";
        URL issueURL = new URL(JIRA_URL_BASE + issueNumber);
        JiraIssue issue = (JiraIssue) jiraHelper.findIssue(issueURL);
        assertEquals(issue.getStatus(), JiraIssue.IssueStatus.RESOLVED.toString());
    }

    public void testGetFlags() throws Exception {
        // Test the flags on an already resolved JIRA issue, EAP5-55.
        String issueNumber = "EAP5-55";
        URL issueURL = new URL(JIRA_URL_BASE + issueNumber);
        JiraIssue issue = (JiraIssue) jiraHelper.findIssue(issueURL);
        List<Flag> flags = issue.getFlags();

        // CDW Release
        Flag release = flags.get(0);
        assertEquals(release.getName(), "CDW release");
        assertEquals(release.getStatus(), Flag.Status.POSITIVE);
        assertEquals(release.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag pm = flags.get(1);
        assertEquals(pm.getName(), "CDW pm_ack");
        assertEquals(pm.getStatus(), Flag.Status.POSITIVE);
        assertEquals(pm.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag devel = flags.get(2);
        assertEquals(devel.getName(), "CDW devel_ack");
        assertEquals(devel.getStatus(), Flag.Status.POSITIVE);
        assertEquals(devel.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag qa = flags.get(3);
        assertEquals(qa.getName(), "CDW qa_ack");
        assertEquals(qa.getStatus(), Flag.Status.POSITIVE);
        assertEquals(qa.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag blocker = flags.get(4);
        assertEquals(blocker.getName(), "CDW blocker");
        assertEquals(blocker.getStatus(), Flag.Status.UNKNOWN);
        assertEquals(blocker.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag exception = flags.get(5);
        assertEquals(exception.getName(), "CDW exception");
        assertEquals(exception.getStatus(), Flag.Status.UNKNOWN);
        assertEquals(exception.getSetter(), "{UNKNOWN_SETTER}");
    }

    public void testGetResolution() throws Exception {
        String issueNumber = "EAP5-40";
        URL issueURL = new URL(JIRA_URL_BASE + issueNumber);
        JiraIssue issue = (JiraIssue) jiraHelper.findIssue(issueURL);
        assertEquals(issue.getResolution(), "WON'T FIX");
    }

    public void testGetFixVersion() throws Exception {
        String issueNumber = "EAP5-54";
        URL issueURL = new URL(JIRA_URL_BASE + issueNumber);
        JiraIssue issue = (JiraIssue) jiraHelper.findIssue(issueURL);

        // We know that there will be one fix version for this issue. Which is 5.3.0.GA
        Set<String> fixVersions = issue.getFixVersions();
        assertEquals(fixVersions.size(), 1);
        assertTrue(fixVersions.contains("5.3.0.GA"));
    }

}
