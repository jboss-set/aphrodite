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

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.spi.IssueHome;
import org.jboss.set.aphrodite.spi.AphroditeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/23/17.
 */
public class IssueHomeImpl implements IssueHome {
    public static final String JBEAPProject = "JBoss Enterprise Application Platform";

    @Override
    public Stream<Issue> findUpstreamReferences(Issue issue) {
        if (! (issue instanceof JiraIssue))
            return null;

        return filterUpstreamReferences(loadCloneIssues((JiraIssue) issue), (JiraIssue) issue);
    }

    public Stream<Issue> filterUpstreamReferences(List<Issue> cloneIssues, JiraIssue downstreamIssue) {
        List<Issue> upstreamReferences = new ArrayList<>();
        cloneIssues.stream().filter(i -> isUpstreamIssue((JiraIssue) i, downstreamIssue)).forEach(upstreamReferences::add);
        return upstreamReferences.stream();
    }

    private List<Issue> loadCloneIssues(JiraIssue jiraIssue) {
        List<Issue> issues = null;

        try {
            issues = Aphrodite.instance().getIssues(jiraIssue.getClones());
        } catch (AphroditeException e) {
            e.printStackTrace();
        }

        return (issues != null) ? issues : new ArrayList<>();
    }

    public static boolean isUpstreamIssue(JiraIssue upstreamIssue, JiraIssue downstreamIssue) {
        if (upstreamIssue == null || downstreamIssue == null || ! upstreamIssue.getSummary().equals(downstreamIssue.getSummary()))
            return false;

        if (!isIssueJBEAP(upstreamIssue))
            return true;

        String upstreamRelease = extractTargetRelease(upstreamIssue.getStreamStatus());
        String downstreamRelease = extractTargetRelease(downstreamIssue.getStreamStatus());

        return VersionComparator.isFirstVersionHigher(upstreamRelease, downstreamRelease);
    }

    public static boolean isIssueJBEAP(JiraIssue issue) {
        return issue != null && issue.getProduct() != null && issue.getProduct().isPresent()
                && issue.getProduct().get().equals(JBEAPProject);
    }

    private static String extractTargetRelease(Map<String, FlagStatus> streamStatus) {
        // There should be max 1 key with value = FlagStatus.ACCEPTED or null in the stream status
        return (streamStatus != null && streamStatus.size() > 0) ? streamStatus.keySet().iterator().next() : "";
    }

}
