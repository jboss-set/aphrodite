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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.spi.IssueHome;
import org.jboss.set.aphrodite.spi.AphroditeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/23/17.
 */
public class JiraIssueHomeImpl implements IssueHome {
    public static final String JBEAPProject = "JBoss Enterprise Application Platform";
    private static final Log LOG = LogFactory.getLog(JiraIssueHomeImpl.class);

    @Override
    public Stream<Issue> findUpstreamReferences(Issue issue) {
        if (!(issue instanceof JiraIssue))
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
            Utils.logException(LOG, e);
        }

        return (issues != null) ? issues : new ArrayList<>();
    }

    public static boolean isUpstreamIssue(JiraIssue upstreamIssue, JiraIssue downstreamIssue) {
        if (upstreamIssue == null || downstreamIssue == null)
            return false;

        if (!isIssueJBEAP(upstreamIssue))
            return true;

        if (!matchesSuffix(upstreamIssue.getSummary(), downstreamIssue.getSummary()))
            return false;

        String v1 = extractTargetRelease(upstreamIssue.getStreamStatus());
        String v2 = extractTargetRelease(downstreamIssue.getStreamStatus());

        if (!isMajroAndMinorVersionNumeric(v1) || !isMajroAndMinorVersionNumeric(v2))
            return false;

        v1 = getMajorAndMinorOf(v1);
        v2 = getMajorAndMinorOf(v2);

        return VersionComparator.INSTANCE.compare(v1, v2) > 0;
    }

    private static boolean matchesSuffix(Optional<String> summary, Optional<String> summary1) {
        if (!summary.isPresent() && !summary1.isPresent())
            return true;
        else if (summary.isPresent() && summary1.isPresent()) {
            int endOfPrefix = summary.get().indexOf(")") + 1;
            int endOfPrefix1 = summary1.get().indexOf(")") + 1;
            String trimmedSummaryWithoutPrefix = summary.get().substring(endOfPrefix).trim();
            String trimmedSummaryWithoutPrefix1 = summary1.get().substring(endOfPrefix1).trim();
            return trimmedSummaryWithoutPrefix.equals(trimmedSummaryWithoutPrefix1);
        }

        return false;
    }

    private static boolean isMajroAndMinorVersionNumeric(String version) {
        return version.matches("^[0-9]+\\.[0-9]+\\..*$");
    }

    private static String getMajorAndMinorOf(String version) {
        int indexOfSecondDot = version.indexOf(".", version.indexOf(".") + 1);
        return version.substring(0, indexOfSecondDot + 1);
    }

    public static boolean isIssueJBEAP(JiraIssue issue) {
        return issue != null && issue.getProduct().isPresent() && issue.getProduct().get().equals(JBEAPProject);
    }

    private static String extractTargetRelease(Map<String, FlagStatus> streamStatus) {
        // There should be max 1 key with value = FlagStatus.ACCEPTED or null in the stream status
        return (streamStatus.size() > 0) ? streamStatus.keySet().iterator().next() : "";
    }

}
