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

import junit.framework.Assert;
import org.jboss.pull.shared.connectors.common.Flag;
import org.jboss.pull.shared.connectors.jira.JiraHelper;
import org.jboss.pull.shared.connectors.jira.JiraIssue;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * @author navssurtani
 *
 * Simple class to setup and test the methods for the {@link org.jboss.pull.shared.connectors.jira.JiraHelper}
 */
@Test
public class JiraHelperTest {

    private static final String BASE_FILE_PROPERTY = "configuration.base.file";
    private static final String BASE_FILE_NAME = "./processor-eap-6.properties.example";

    private JiraHelper jiraHelper = null;


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
        JiraIssue issue = jiraHelper.getJIRA(issueNumber);
        Assert.assertEquals(issueNumber, issue.getNumber());
    }

    public void testGetStatus() throws Exception {
        String issueNumber = "EAP5-10";
        JiraIssue issue = jiraHelper.getJIRA(issueNumber);
        Assert.assertEquals(issue.getStatus(), JiraIssue.IssueStatus.RESOLVED.toString());
    }

    public void testGetFlags() throws Exception {
        // Test the flags on an already resolved JIRA issue, EAP5-55.
        String issueNumber = "EAP5-55";
        JiraIssue issue = jiraHelper.getJIRA(issueNumber);
        List<Flag> flags = issue.getFlags();

        // CDW Release
        Flag release = flags.get(0);
        Assert.assertEquals(release.getName(), "CDW release");
        Assert.assertEquals(release.getStatus(), Flag.Status.POSITIVE);
        Assert.assertEquals(release.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag pm = flags.get(1);
        Assert.assertEquals(pm.getName(), "CDW pm_ack");
        Assert.assertEquals(pm.getStatus(), Flag.Status.POSITIVE);
        Assert.assertEquals(pm.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag devel = flags.get(2);
        Assert.assertEquals(devel.getName(), "CDW devel_ack");
        Assert.assertEquals(devel.getStatus(), Flag.Status.POSITIVE);
        Assert.assertEquals(devel.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag qa = flags.get(3);
        Assert.assertEquals(qa.getName(), "CDW qa_ack");
        Assert.assertEquals(qa.getStatus(), Flag.Status.POSITIVE);
        Assert.assertEquals(qa.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag blocker = flags.get(4);
        Assert.assertEquals(blocker.getName(), "CDW blocker");
        Assert.assertEquals(blocker.getStatus(), Flag.Status.UNKNOWN);
        Assert.assertEquals(blocker.getSetter(), "{UNKNOWN_SETTER}");

        // CDW Release
        Flag exception = flags.get(5);
        Assert.assertEquals(exception.getName(), "CDW exception");
        Assert.assertEquals(exception.getStatus(), Flag.Status.UNKNOWN);
        Assert.assertEquals(exception.getSetter(), "{UNKNOWN_SETTER}");
    }

    public void testGetResolution() throws Exception {
        String issueNumber = "EAP5-40";
        JiraIssue issue = jiraHelper.getJIRA(issueNumber);
        Assert.assertEquals(issue.getResolution(), "WON'T FIX");
    }

    public void testGetFixVersion() throws Exception {
        String issueNumber = "EAP5-54";
        JiraIssue issue = jiraHelper.getJIRA(issueNumber);

        // We know that there will be one fix version for this issue. Which is 5.3.0.GA
        Set<String> fixVersions = issue.getFixVersions();
        Assert.assertEquals(fixVersions.size(), 1);
        Assert.assertTrue(fixVersions.contains("5.3.0.GA"));
    }

}
