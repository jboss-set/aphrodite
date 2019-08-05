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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/17/19.
 */
public class CandidateRelease {
    private List<Issue> issues;

    public static final Pattern GA_VERSION = Pattern.compile("^[7-9]+\\.[0-9]+\\.[0-9]+\\.GA$");

    public static final Pattern VERSION_PART = Pattern.compile("^[7-9]+\\.[0-9]+\\.[0-9]+");

    public static final Pattern CR_VERSION = Pattern.compile("^[7-9]*\\.[0-9]*\\.[0-9]*\\.CR[0-9]+$");


    public static boolean isGA(String releaseCandidateName) {
        return GA_VERSION.matcher(releaseCandidateName).find();
    }

    public static boolean isCR(String releaseCandidateName) { return CR_VERSION.matcher(releaseCandidateName).find(); }

    Version releaseCandidateVersion;

    public CandidateRelease(Version version) {
        releaseCandidateVersion = version;
    }

    public static String extractVersion(String name) throws NotFoundException {
        Matcher matcher = VERSION_PART.matcher(name);

        if(!matcher.find()) {
            throw new NotFoundException();
        }

        return matcher.group();
    }

    public List<Issue> getIssues() throws NameNotFoundException {
        if (issues == null) {
            //fetch
            JiraIssueTracker issueTrackerService = SimpleContainer.instance().lookup(JiraIssueTracker.class.getSimpleName(), JiraIssueTracker.class);
            issues = issueTrackerService.getIssues(releaseCandidateVersion);
        }
        return issues;
    }










}
