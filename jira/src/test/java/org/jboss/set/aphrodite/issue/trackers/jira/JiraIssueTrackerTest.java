/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.issue.trackers.jira;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.Version;

/**
 * @author wangc
 *
 */
public class JiraIssueTrackerTest {
    private static final String PROJECT = "JBEAP";
    private static List<Version> versions;

    private JiraIssueTracker jiraIssueTracker;

    static {
        try {
            versions = Arrays.asList(
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12321329"), (long) 12321329, "6.1.0.Alpha1", "EAP 6.1 Alpha release for the Community", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12321661"), (long) 12321661, "6.1.0.CR1", "EAP 6.1 Candidate Release", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12335686"), (long) 12335686, "7.0.10.GA", "EAP 7.0.10.GA Payload", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12331747"), (long) 12331747, "", "7.0.3.CR3-doc", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12329491"), (long) 12329491, "7.0.z.GA", "EAP 7.0.z Release Stream", false, false, new DateTime()),                    
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12332890"), (long) 12332890, "7.1.1.GA", "EAP 7.1.1.GA Payload", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12332773"), (long) 12332773, "7.2.0.GA", "EAP 7.2.0.GA Release", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12334325"), (long) 12334325, "7.1.z.GA", "EAP 7.1.z Release Stream", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12331159"), (long) 12331159, "7.Doc.Test", "Test of Docs fix version", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12330663"), (long) 12330663, "Individual Patches.GA", "Patches created for individual customers by the support team.", false, false, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12329564"), (long) 12329564, "7.backlog.GA", "EAP 7.x.z backlog", false, false, new DateTime())
                    );
        } catch (URISyntaxException e) {
            // ignore
        }
    }

    @Before
    public void setup() {
        jiraIssueTracker = mock(JiraIssueTracker.class);
        mockJiraIssueTracker();
    }

    private void mockJiraIssueTracker() {
        when(jiraIssueTracker.getVersionsByProject(PROJECT)).thenReturn(versions);
    }

    @Test
    public void testGetVersionsByProject() {
        List<Version> result = jiraIssueTracker.getVersionsByProject(PROJECT);
        assertEquals("Should be equal", versions.size(), result.size());
    }
}
