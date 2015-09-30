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

import net.rcarz.jiraclient.Component;
import net.rcarz.jiraclient.IssueLink;
import net.rcarz.jiraclient.Project;
import net.rcarz.jiraclient.User;
import net.rcarz.jiraclient.Version;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueTracking;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.DEV_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.JSON_CUSTOM_FIELD;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.PM_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.QE_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.TARGET_RELEASE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getAphroditeStatus;

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    Issue jiraSearchIssueToIssue(URL baseURL, net.rcarz.jiraclient.Issue jiraIssue) {
        URL url = trackerIdToBrowsableUrl(baseURL, jiraIssue.getKey());
        return jiraIssueToIssue(url, jiraIssue);
    }

    Issue jiraIssueToIssue(URL url, net.rcarz.jiraclient.Issue jiraIssue) {
        Issue issue = new Issue(url);

        issue.setTrackerId(jiraIssue.getKey());
        issue.setDescription(jiraIssue.getDescription());
        issue.setStatus(getAphroditeStatus(jiraIssue.getStatus().getName()));
        issue.setTracking(new IssueTracking(jiraIssue.getTimeEstimate(), jiraIssue.getTimeSpent()));

        // TODO implement streams when it is in JIRA
        setIssueProject(issue, jiraIssue);
        setIssueComponent(issue, jiraIssue);
        setIssueAssignee(issue, jiraIssue);
        setIssueStage(issue, jiraIssue);
        setIssueType(issue, jiraIssue);
        setIssueRelease(issue, jiraIssue);
        setIssueDependencies(url, issue, jiraIssue.getIssueLinks());
        setIssueComments(issue, jiraIssue);

        return issue;
    }

    private void setIssueProject(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        Project project = jiraIssue.getProject();
        if (project != null)
            issue.setProduct(project.getName());
    }

    private void setIssueComponent(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        List<Component> components = jiraIssue.getComponents();
        if (components != null && !components.isEmpty())
            issue.setComponent(components.get(0).getName());
    }

    private void setIssueAssignee(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        User assignee = jiraIssue.getAssignee();
        if (assignee != null)
            issue.setAssignee(assignee.getName());
    }

    private void setIssueStage(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        Stage stage = new Stage();
        stage.setStatus(Flag.PM, FlagStatus.getMatchingFlag(jiraIssue.getField(JSON_CUSTOM_FIELD + PM_ACK)));
        stage.setStatus(Flag.DEV, FlagStatus.getMatchingFlag(jiraIssue.getField(JSON_CUSTOM_FIELD + DEV_ACK)));
        stage.setStatus(Flag.QE, FlagStatus.getMatchingFlag(jiraIssue.getField(JSON_CUSTOM_FIELD + QE_ACK)));
        issue.setStage(stage);
    }

    private void setIssueType(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        String type = jiraIssue.getIssueType().getName();
        try {
            issue.setType(IssueType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException e) {
            issue.setType(IssueType.UNDEFINED);
        }
    }

    private void setIssueRelease(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        Release release = new Release();
        Object jsonField = jiraIssue.getField(JSON_CUSTOM_FIELD + TARGET_RELEASE);
        if (jsonField instanceof JSONNull)
            return;

        JSONObject jsonObject = (JSONObject) jsonField;
        String milestone = (String) jsonObject.get("name");
        release.setMilestone(milestone);

        List<Version> versions = jiraIssue.getVersions();
        if (versions != null && !versions.isEmpty())
            release.setVersion(versions.get(0).getName());
        issue.setRelease(release);
    }


    private void setIssueDependencies(URL originalUrl, Issue issue, List<IssueLink> links) {
        if (links == null || links.isEmpty())
            return;

        for (IssueLink il : links) {
            net.rcarz.jiraclient.Issue linkedIssue =
                    il.getInwardIssue() != null ? il.getInwardIssue() : il.getOutwardIssue();

            URL url = trackerIdToBrowsableUrl(originalUrl, linkedIssue.getKey());
            switch (il.getType().getName().toLowerCase()) {
                case "dependency":
                    issue.getDependsOn().add(url);
                    break;
                case "blocks":
                    issue.getBlocks().add(url);
                    break;
            }
        }
    }

    private void setIssueComments(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        List<Comment> comments = new ArrayList<>();
        jiraIssue.getComments().forEach(c -> comments.add(new Comment(c.getId(), c.getBody(), false)));
        issue.getComments().addAll(comments);
    }

    private URL trackerIdToBrowsableUrl(URL url, String trackerId) {
        try {
            String link = url.getProtocol() + "://" + url.getHost() + BROWSE_ISSUE_PATH + trackerId;
            return new URL(link);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
