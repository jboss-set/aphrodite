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


package org.jboss.set.aphrodite.issue.trackers.jira;

import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.Component;
import net.rcarz.jiraclient.IssueType;
import net.rcarz.jiraclient.Project;
import net.rcarz.jiraclient.Status;
import net.rcarz.jiraclient.User;
import net.rcarz.jiraclient.Version;
import net.sf.json.JSONObject;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.issue.trackers.util.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class JiraIssueWrapperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static String JIRA_URL = "https://issues.jboss.org/";
    private IssueWrapper issueWrapper;
    private URL jiraURL;

    @Mock
    private net.rcarz.jiraclient.Issue jiraIssue01;

    private Issue issue01;

    @Before
    public void setUp() throws MalformedURLException {
        issueWrapper = new IssueWrapper();

        jiraURL = new URL(JIRA_URL);
        mockJiraIssue01();

        issue01 =createTestIssue01(jiraURL);
    }

    @Test
    public void validJIRAToIssueTest() {
        Issue result = issueWrapper.jiraIssueToIssue(jiraURL, jiraIssue01);

        assertNotNull(result);
        TestUtils.assertDeepEqualsIssue(issue01, result);
    }

    @Test
    public void nullJIRAToIssueTest() {
        expectedException.expect(NullPointerException.class);

        Issue result = issueWrapper.jiraIssueToIssue(jiraURL, null);
        assertNull(result);
    }

    @Test
    public void nullURLJIRAToIssueTest() {
        expectedException.expect(IllegalArgumentException.class);

        Issue result = issueWrapper.jiraIssueToIssue(null, jiraIssue01);
        assertNull(result);
    }

    private void mockJiraIssue01() {
        when(jiraIssue01.getKey()).thenReturn("1111111");
        when(jiraIssue01.getDescription()).thenReturn("Test jira");

        Status statusMock = mock(Status.class);
        when(statusMock.getName()).thenReturn("open");

        when(jiraIssue01.getStatus()).thenReturn(statusMock);
        when(jiraIssue01.getTimeEstimate()).thenReturn(8);
        when(jiraIssue01.getTimeSpent()).thenReturn(8);

        Project projectMock = mock(Project.class);
        when(projectMock.getName()).thenReturn("EAP");
        when(jiraIssue01.getProject()).thenReturn(projectMock);

        Component componentMock = mock(Component.class);
        when(componentMock.getName()).thenReturn("CLI");
        when(jiraIssue01.getComponents()).thenReturn(Collections.singletonList(componentMock));

        User assigneeMock = mock(User.class);
        when(assigneeMock.getEmail()).thenReturn("jboss-set@redhat.com");
        when(jiraIssue01.getAssignee()).thenReturn(assigneeMock);

        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.PM_ACK)).thenReturn("+");
        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.DEV_ACK)).thenReturn("+");
        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.QE_ACK)).thenReturn("+");

        JSONObject release = new JSONObject();
        release.put("name", "---");
        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.TARGET_RELEASE)).thenReturn(
                release
        );

        Version versionMock = mock(Version.class);
        when(versionMock.getName()).thenReturn("6.4.4");
        when(jiraIssue01.getVersions()).thenReturn(Collections.singletonList(versionMock));

        IssueType issueTypeMock = mock(IssueType.class);
        when(issueTypeMock.getName()).thenReturn("bug");
        when(jiraIssue01.getIssueType()).thenReturn(issueTypeMock);

        Comment commentMock = mock(Comment.class);
        when(commentMock.getId()).thenReturn("1");
        when(commentMock.getBody()).thenReturn("comment body");
        when(jiraIssue01.getComments()).thenReturn(Collections.singletonList(commentMock));
    }

    private Issue createTestIssue01(URL url) throws MalformedURLException {
        Issue result = new Issue(url);

        result.setTrackerId("1111111");
        result.setAssignee("jboss-set@redhat.com");
        result.setDescription("Test jira");
        result.setStatus(IssueStatus.NEW);
        result.setComponent("CLI");
        result.setProduct("EAP");
        result.setType(org.jboss.set.aphrodite.domain.IssueType.BUG);
        result.setRelease(new Release("6.4.4", "---"));
        result.setDependsOn(Collections.emptyList());
        result.setBlocks(Collections.emptyList());
        result.setEstimation(new IssueEstimation(8.0, 8.0));

        Stage issueStage = new Stage();
        issueStage.setStatus(Flag.DEV, FlagStatus.ACCEPTED);
        issueStage.setStatus(Flag.QE, FlagStatus.ACCEPTED);
        issueStage.setStatus(Flag.PM, FlagStatus.ACCEPTED);

        result.setStage(issueStage);

        return result;
    }

}
