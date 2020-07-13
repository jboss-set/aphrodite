/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import com.atlassian.jira.rest.client.api.domain.Version;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.NotFoundException;

import javax.naming.NameNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/17/19.
 */
// JiraRelease is fix version with following form "x.x.x.GA"
// There can be multiple CRs in one release which have fix version in form "x.x.x.GA.CRx"
// The CRs can contain issues which are not linked to the "x.x.x.GA" fix version
public class JiraRelease {
    private static final String PROJECT_NAME = "JBEAP";

    private Version version;

    private List<CandidateRelease> candidateReleases;

    private Set<Issue> issuesInRelease;

    public JiraRelease(Version version, List<CandidateRelease> candidateReleases) {
        this.version = version;
        this.candidateReleases = candidateReleases;
    }

    private void addCandidateRelease(CandidateRelease cr) {
        candidateReleases.add(cr);
    }

    public List<CandidateRelease> getCandidateReleases() {
        return candidateReleases;
    }

    public Set<Issue> getIssues() {
        if (issuesInRelease == null) {
            Set<Issue> issues = new HashSet<>();

            candidateReleases.forEach(candidateRelease -> {
                try {
                    issues.addAll(candidateRelease.getIssues());
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            });
            issuesInRelease = issues;

        }
        return issuesInRelease;
    }

    public static Collection<JiraRelease> findAll() throws NameNotFoundException {
        Map<String, JiraRelease> releases = new HashMap<>();

        JiraIssueTracker issueTrackerService = SimpleContainer.instance().lookup(JiraIssueTracker.class.getSimpleName(), JiraIssueTracker.class);

        //Find all fix version with x.x.x.GA or with x.x.x.CRx

        Iterable<Version> versions = issueTrackerService.getVersionsByProject(PROJECT_NAME);

        versions.forEach(version -> {
            if (CandidateRelease.isGA(version.getName())) {
                try {
                    JiraRelease release = new JiraRelease(version, new ArrayList<>());
                    release.addCandidateRelease(new CandidateRelease(version));
                    releases.put(CandidateRelease.extractVersion(version.getName()), release);

                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

            }
        });

        versions.forEach(version -> {
            if (CandidateRelease.isCR(version.getName())) {
                try {
                    String nameGA = CandidateRelease.extractVersion(version.getName());
                    if(releases.containsKey(nameGA)) {
                        releases.get(nameGA).addCandidateRelease(new CandidateRelease(version));
                    }
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        return releases.values();
    }

}
