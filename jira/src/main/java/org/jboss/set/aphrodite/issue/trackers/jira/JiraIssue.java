/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Issue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.set.aphrodite.config.TrackerType.JIRA;

/**
 * Created by Romain Pelisse <belaran@redhat.com> on 20/04/16.
 */
public class JiraIssue extends Issue {

    private List<URL> pullRequests = new ArrayList<URL>();

    private String sprintRelease = "";

    private JiraIssueResolution resolution;

    private List<JiraLabel> labels = new ArrayList<>();

    private List<JiraChangelogGroup> changelog = new ArrayList<>();

    public JiraIssue(final URL url) {
        super(url, JIRA);
    }

    @Deprecated
    public JiraIssue(URL url, TrackerType type) {
        super(url, type);
        if (!type.equals(JIRA))
            throw new IllegalStateException("Can't instantiate if issue is not of JIRA type");
    }

    public List<URL> getPullRequests() {
        return pullRequests;
    }

    public void setPullRequests(List<URL> pullRequests) {
        this.pullRequests = pullRequests;
    }

    public String getSprintRelease() {
        return sprintRelease;
    }

    public void setSprintRelease(String sprintRelease) {
        this.sprintRelease = sprintRelease;
    }

    public JiraIssueResolution getResolution() {
        return resolution;
    }

    public void setResolution(JiraIssueResolution resolution) {
        this.resolution = resolution;
    }

    public List<JiraLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<JiraLabel> labels) {
        this.labels = labels;
    }

    public List<JiraChangelogGroup> getChangelog() {
        return changelog;
    }

    public void setChangelog(List<JiraChangelogGroup> changelog) {
        this.changelog = changelog;
    }

}
