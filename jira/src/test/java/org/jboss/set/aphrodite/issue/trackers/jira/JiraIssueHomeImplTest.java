/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraIssueHomeImpl.isUpstreamIssue;
import static org.junit.Assert.assertEquals;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/23/17.
 */
public class JiraIssueHomeImplTest {


    @Test
    public void testIsUpstreamIssue() {
        assertEquals("Issue should be upstream.", true, isUpstreamIssue(createUpstream(), createDownstream()));
        assertEquals("Issue should be upstream.", true, isUpstreamIssue(createUpstreamCommunity(), createDownstream()));
        assertEquals("Issue should not be upstream.", false, isUpstreamIssue(createDownstream(), createDownstream()));
        assertEquals("Issue should not be upstream.", false, isUpstreamIssue(null, null));
        assertEquals("Issue should not be upstream.", false, isUpstreamIssue(createDownstream(), createIndividualPatches()));
    }

    private JiraIssue createDownstream() {
        JiraIssue issue = mockIssue(JiraIssueHomeImpl.JBEAPProject, "Summary.", "7.0.7.GA");
        return issue;
    }

    private JiraIssue createUpstream() {
        JiraIssue issue = mockIssue(JiraIssueHomeImpl.JBEAPProject, "Summary.", "7.1.0.GA");

        return issue;
    }

    private JiraIssue createIndividualPatches() {
        JiraIssue issue = mockIssue(JiraIssueHomeImpl.JBEAPProject, "Summary.", "IndividualPatches GA");

        return issue;
    }

    private JiraIssue createUpstreamCommunity() {
        JiraIssue issue = mockIssue("WildFly Core", "Summary.", "");

        return issue;
    }

    private JiraIssue mockIssue(String project, String summary, String targetRelease) {
        JiraIssue result = Mockito.mock(JiraIssue.class);
        Mockito.when(result.getProduct()).thenReturn(Optional.ofNullable(project));
        Mockito.when(result.getSummary()).thenReturn(Optional.ofNullable(summary));
        Mockito.when(result.getStreamStatus()).thenReturn(Collections.singletonMap(targetRelease, FlagStatus.ACCEPTED));

        return result;
    }

    @Test
    public void testFindUpstreamReferences() {
        testFindUpstreamReference(createUpstream());
        testFindUpstreamReference(createUpstreamCommunity());
        testNoUpstreamReference(createIndividualPatches());
        testNoUpstreamReference(createDownstream());
        testNoUpstreamReference(null);
    }

    private void testFindUpstreamReference(JiraIssue upstream) {
        List<Issue> issues = findUpstreamReference(upstream);
        assertEquals(1, issues.size());
        assertEquals(upstream, issues.get(0));
    }

    private List<Issue> findUpstreamReference(JiraIssue upstream) {
        JiraIssueHomeImpl issueHome = new JiraIssueHomeImpl();
        JiraIssue downstream = createDownstream();
        List<Issue> linkedCloneIssues = new ArrayList<>();

        linkedCloneIssues.add(upstream);

        return issueHome.filterUpstreamReferences(linkedCloneIssues, downstream).collect(Collectors.toList());
    }

    private void testNoUpstreamReference(JiraIssue upstream) {
        List<Issue> issues = findUpstreamReference(upstream);
        assertEquals(0, issues.size());
    }

}