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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssuePriority;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.domain.User;
import org.jboss.set.aphrodite.issue.trackers.util.TestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Component;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.TimeTracking;
import com.atlassian.jira.rest.client.api.domain.Version;

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
    private com.atlassian.jira.rest.client.api.domain.Issue jiraIssue01;
    
    @Mock
    private TimeTracking timeTracking;

    private DateTime genericDateTime;

    @Mock
    private IssueField pmAckField;

    @Mock
    private IssueField devAckField;

    @Mock
    private IssueField qaAckField;

    @Mock
    private IssueField targetField;

    @Mock
    private IssueField createdField;

    private Issue issue01;

    @Before
    public void setUp() throws MalformedURLException, ParseException {
        issueWrapper = new IssueWrapper();
        genericDateTime = new DateTime(2015, 12, 29, 15, 16, 50, 946, DateTimeZone.forOffsetHours(1));
        jiraURL = new URL(JIRA_URL);
        mockJiraIssue01();

        issue01 = createTestIssue01(jiraURL);
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

        BasicPriority priority = mock(BasicPriority.class);
        when(priority.getName()).thenReturn("MAJOR");
        
        when(jiraIssue01.getSummary()).thenReturn("Test Issue");
        when(jiraIssue01.getStatus()).thenReturn(statusMock);
        when(jiraIssue01.getPriority()).thenReturn(priority);
        when(jiraIssue01.getTimeTracking()).thenReturn(timeTracking);
        when(timeTracking.getOriginalEstimateMinutes()).thenReturn(480);
        when(timeTracking.getTimeSpentMinutes()).thenReturn(480);

        Project projectMock = mock(Project.class);
        when(projectMock.getName()).thenReturn("EAP");
        when(jiraIssue01.getProject()).thenReturn(projectMock);

        Component componentMock = mock(Component.class);
        when(componentMock.getName()).thenReturn("CLI");
        when(jiraIssue01.getComponents()).thenReturn(Collections.singletonList(componentMock));

        com.atlassian.jira.rest.client.api.domain.User assigneeMock = mock(com.atlassian.jira.rest.client.api.domain.User.class);
        when(assigneeMock.getName()).thenReturn("jboss-set");
        when(assigneeMock.getEmailAddress()).thenReturn("jboss-set@redhat.com");
        when(jiraIssue01.getAssignee()).thenReturn(assigneeMock);

        when(jiraIssue01.getCreationDate()).thenReturn(genericDateTime);
        when(jiraIssue01.getUpdateDate()).thenReturn(genericDateTime);


        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.PM_ACK)).thenReturn(pmAckField);
        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.DEV_ACK)).thenReturn(devAckField);
        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.QE_ACK)).thenReturn(qaAckField);
        when(pmAckField.getValue()).thenReturn("+");
        when(devAckField.getValue()).thenReturn("+");
        when(qaAckField.getValue()).thenReturn("+");


        when(jiraIssue01.getField(JiraFields.JSON_CUSTOM_FIELD + JiraFields.TARGET_RELEASE)).thenReturn(targetField);
        JSONObject targetObject = new JSONObject();
        try {
            targetObject.put("name", "7.0.z");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        when(targetField.getValue()).thenReturn(targetObject);

        Version versionMock = mock(Version.class);
        when(versionMock.getName()).thenReturn("6.4.4");
        when(jiraIssue01.getFixVersions()).thenReturn(Collections.singletonList(versionMock));

        IssueType issueTypeMock = mock(IssueType.class);
        when(issueTypeMock.getName()).thenReturn("bug");
        when(jiraIssue01.getIssueType()).thenReturn(issueTypeMock);

        Comment commentMock = mock(Comment.class);
        when(commentMock.getId()).thenReturn(1L);
        when(commentMock.getBody()).thenReturn("comment body");
        when(jiraIssue01.getComments()).thenReturn(Collections.singletonList(commentMock));
    }

    private Issue createTestIssue01(URL url) throws ParseException {
        Issue result = new Issue(url, TrackerType.JIRA);

        result.setTrackerId("1111111");
        result.setSummary("Test Issue");
        result.setCreationTime(new SimpleDateFormat(JiraFields.DATE_STRING_FORMAT)
                .parse("2013-01-17T00:12:31.000-0500"));
        result.setAssignee(new User("jboss-set@redhat.com", "jboss-set"));
        result.setDescription("Test jira");
        result.setStatus(IssueStatus.NEW);
        result.setPriority(IssuePriority.MAJOR);
        result.setComponents(Collections.singletonList("CLI"));
        result.setProduct("EAP");
        result.setType(org.jboss.set.aphrodite.domain.IssueType.BUG);

        List<Release> releases = new ArrayList<>();
        releases.add(new Release("6.4.4"));
        result.setReleases(releases);

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

    @Test
    public void jiraLabelsFromURLJiraToIssueTest() {
        Set<String> testLabels = null;
        URLJiraToIssueWithLabelTest(testLabels);

        testLabels = new HashSet<>();
        URLJiraToIssueWithLabelTest(testLabels);

        testLabels.add("Label#1");
        URLJiraToIssueWithLabelTest(testLabels);

        testLabels.add("Label#2");
        URLJiraToIssueWithLabelTest(testLabels);

    }

    private void URLJiraToIssueWithLabelTest(Set<String> labelsFromJira) {
        JiraIssue result;
        int countOfLabels = (labelsFromJira != null) ? labelsFromJira.size() : 0;

        mockLabelsToJiraIssue01(labelsFromJira);
        result = (JiraIssue) issueWrapper.jiraIssueToIssue(jiraURL, jiraIssue01);
        assertNotNull(result);
        assertEquals(result.getLabels().size(), countOfLabels);
        if (labelsFromJira != null && labelsFromJira.size() > 0 ) {
            labelsFromJira.forEach(label -> assertTrue(result.getLabels().contains(new JiraLabel(label))));
        }
    }

    private void mockLabelsToJiraIssue01(Set<String> labels) {
        when(jiraIssue01.getLabels()).thenReturn(labels);
    }

}
