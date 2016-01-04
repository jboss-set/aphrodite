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
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.IssueLink;
import net.rcarz.jiraclient.Project;
import net.rcarz.jiraclient.User;
import net.rcarz.jiraclient.Version;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.CREATED_FIELD;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.UPDATED_FIELD;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.DATE_STRING_FORMAT;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.DEV_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.FLAG_MAP;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.JSON_CUSTOM_FIELD;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.PM_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.QE_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.TARGET_RELEASE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getAphroditeStatus;

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    private static final Log LOG = LogFactory.getLog(JiraIssueTracker.class);

    Issue jiraSearchIssueToIssue(URL baseURL, net.rcarz.jiraclient.Issue jiraIssue) {
        URL url = trackerIdToBrowsableUrl(baseURL, jiraIssue.getKey());
        return jiraIssueToIssue(url, jiraIssue);
    }

    private void setCreationTime(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        issue.setCreationTime(getDateField(jiraIssue, CREATED_FIELD));
    }

    private Date getDateField(net.rcarz.jiraclient.Issue jiraIssue, String fieldName) {
        final String dateAsString = (String) jiraIssue.getField(fieldName);
        try {
            return new SimpleDateFormat(DATE_STRING_FORMAT).parse(dateAsString);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to deserialized date:" + dateAsString, e);
        }
    }

    private void setLastUpdated(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        issue.setLastUpdated(getDateField(jiraIssue, UPDATED_FIELD));
    }

    Issue jiraIssueToIssue(URL url, net.rcarz.jiraclient.Issue jiraIssue) {
        Issue issue = new Issue(url);

        issue.setTrackerId(jiraIssue.getKey());
        issue.setSummary(jiraIssue.getSummary());
        issue.setDescription(jiraIssue.getDescription());
        issue.setStatus(getAphroditeStatus(jiraIssue.getStatus().getName()));

        if (jiraIssue.getTimeEstimate() > 0 || jiraIssue.getTimeSpent() > 0)
            issue.setEstimation(new IssueEstimation(jiraIssue.getTimeEstimate(), jiraIssue.getTimeSpent()));

        // TODO implement streams when it is in JIRA
        setIssueProject(issue, jiraIssue);
        setIssueComponent(issue, jiraIssue);
        setIssueAssignee(issue, jiraIssue);
        setIssueReporter(issue, jiraIssue);

        setIssueStage(issue, jiraIssue);
        setIssueType(issue, jiraIssue);
        setIssueRelease(issue, jiraIssue);
        setIssueDependencies(url, issue, jiraIssue.getIssueLinks());
        setIssueComments(issue, jiraIssue);
        setCreationTime(issue, jiraIssue);
        setLastUpdated(issue, jiraIssue);

        return issue;
    }

    net.rcarz.jiraclient.Issue.FluentUpdate issueToFluentUpdate(Issue issue, net.rcarz.jiraclient.Issue.FluentUpdate update) {
        checkUnsupportedUpdateFields(issue);

        issue.getSummary().ifPresent(summary -> update.field(Field.SUMMARY, summary));
        issue.getComponent().ifPresent(component -> update.field(Field.COMPONENTS, new ArrayList<String>() {{ add(component); }} ));
        issue.getDescription().ifPresent(description -> update.field(Field.DESCRIPTION, description));
        issue.getAssignee().ifPresent(assignee -> update.field(Field.ASSIGNEE, assignee));

        issue.getStage().getStateMap().entrySet()
                .stream()
                .filter(entry -> entry.getValue() != FlagStatus.NO_SET)
                .forEach(entry -> update.field(JSON_CUSTOM_FIELD + FLAG_MAP.get(entry.getKey()),
                        entry.getValue().getSymbol()));

        issue.getRelease().getMilestone().ifPresent(milestone -> update.field(JSON_CUSTOM_FIELD + TARGET_RELEASE, milestone));
        issue.getRelease().getVersion().ifPresent(version ->
            update.field(Field.VERSIONS, new ArrayList<String>() {{
                add(version);
            }}));

        // TODO implement streams when it is in JIRA

        issue.getEstimation().ifPresent(tracking -> {
            update.field(Field.TIME_ESTIMATE, tracking.getInitialEstimate());
            update.field(Field.TIME_SPENT, tracking.getHoursWorked());
        });

        return update;
    }

    private void checkUnsupportedUpdateFields(Issue issue) {
        if (issue.getReporter().isPresent() && LOG.isDebugEnabled())
            LOG.debug("JIRA does not support updating the reporter field, field ignored.");
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
            issue.setAssignee(assignee.getEmail());
    }

    private void setIssueReporter(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        User reporter = jiraIssue.getReporter();
        if (reporter != null)
            issue.setReporter(reporter.getEmail());
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
            if (il.getInwardIssue() != null) {
                net.rcarz.jiraclient.Issue linkedIssue = il.getInwardIssue();
                URL url = trackerIdToBrowsableUrl(originalUrl, linkedIssue.getKey());
                issue.getBlocks().add(url);
            } else {
                net.rcarz.jiraclient.Issue linkedIssue = il.getOutwardIssue();
                URL url = trackerIdToBrowsableUrl(originalUrl, linkedIssue.getKey());
                issue.getDependsOn().add(url);
            }
        }
    }

    private void setIssueComments(Issue issue, net.rcarz.jiraclient.Issue jiraIssue) {
        List<Comment> comments = new ArrayList<>();
        jiraIssue.getComments().forEach(c -> comments.add(new Comment(issue.getTrackerId().get(), c.getId(), c.getBody(), false)));
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
