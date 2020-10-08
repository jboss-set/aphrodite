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

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.DEV_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.FLAG_MAP;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.JSON_CUSTOM_FIELD;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.PM_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.QE_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.SECURITY_LEVEL;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.SECURITY_SENSITIVE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.SECURITY_SENSITIVE_VALUE_TRUE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.TARGET_RELEASE;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getAphroditePriority;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getAphroditeStatus;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.getAphroditeType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.domain.User;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.TimeTracking;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    private static final Log LOG = LogFactory.getLog(JiraIssueTracker.class);

    Issue jiraSearchIssueToIssue(URL baseURL, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        URL url = trackerIdToBrowsableUrl(baseURL, jiraIssue.getKey());
        return jiraIssueToIssue(url, jiraIssue);
    }

    private void setCreationTime(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        issue.setCreationTime(jiraIssue.getCreationDate().toDate());
    }

    private void setLastUpdated(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        issue.setLastUpdated(jiraIssue.getUpdateDate().toDate());
    }

    Issue jiraIssueToIssue(URL url, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        JiraIssue issue = new JiraIssue(url);
        copy(url, jiraIssue, issue);
        return issue;
    }

    void copy(final URL url, final com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, final JiraIssue issue) {
        issue.setTrackerId(jiraIssue.getKey());
        issue.setSummary(jiraIssue.getSummary());
        issue.setDescription(jiraIssue.getDescription());
        String status = jiraIssue.getStatus().getName();
        issue.setStatus(getAphroditeStatus(status), status);
        issue.setPriority(getAphroditePriority(jiraIssue.getPriority().getName()));

        TimeTracking timeTracking = jiraIssue.getTimeTracking();
        if(timeTracking != null) {
            int estimate = (timeTracking.getOriginalEstimateMinutes() == null) ? 0 : timeTracking.getOriginalEstimateMinutes();
            int spent = (timeTracking.getTimeSpentMinutes() == null) ? 0 : timeTracking.getTimeSpentMinutes();
            issue.setEstimation(new IssueEstimation(estimate / 60d, spent / 60d));
        }

        setIssueStream(issue, jiraIssue);
        setIssueProject(issue, jiraIssue);
        setIssueComponent(issue, jiraIssue);

        setIssueUser((i, u) -> i.setAssignee(new User(u.getEmailAddress(), u.getName())), issue, jiraIssue.getAssignee());
        setIssueUser((i, u) -> i.setReporter(new User(u.getEmailAddress(), u.getName())), issue, jiraIssue.getReporter());

        setIssueStage(issue, jiraIssue);
        String type = jiraIssue.getIssueType().getName();
        issue.setType(getAphroditeType(type), type);
        setIssueAffectedVersions(issue, jiraIssue);
        setIssueReleases(issue, jiraIssue);
        setIssueDependencies(url, issue, jiraIssue.getIssueLinks());
        setIssueComments(issue, jiraIssue);
        setCreationTime(issue, jiraIssue);
        setLastUpdated(issue, jiraIssue);
        // Set JIRA specific fields
        setPullRequests(issue, jiraIssue);
        setIssueSprintRelease(issue, jiraIssue);
        setLabels(issue, jiraIssue);
        setChangelog(issue, jiraIssue);
        setResolution(issue, jiraIssue);
        setSecuritySensitive(jiraIssue, issue);
        setSecurityLevel(jiraIssue, issue);
    }

    private void setSecurityLevel(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, JiraIssue issue) {
        IssueField secLevel = jiraIssue.getField(SECURITY_LEVEL);
        if (secLevel != null && secLevel.getValue() != null) {
            JSONObject o = (JSONObject) secLevel.getValue();
            try {
                issue.setSecurityLevel(o.getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setSecuritySensitive(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, JiraIssue issue) {
        IssueField securitySensitiveField = jiraIssue.getField(JSON_CUSTOM_FIELD + SECURITY_SENSITIVE);
        if (securitySensitiveField != null && securitySensitiveField.getValue() != null) {
            JSONArray value = (JSONArray) securitySensitiveField.getValue();
            if (value != null && value.length() > 0) {
                issue.setSecuritySensitiveIssue(true);
            }
        }
    }

    private void setLabels(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        Set<String> jiraLabels = jiraIssue.getLabels();
        List<JiraLabel> labels = new ArrayList<>();

        if (jiraLabels != null)
            jiraLabels.forEach(name -> labels.add(new JiraLabel(name)));

        issue.setLabels(labels);
    }

    private void setChangelog(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        List<JiraChangelogGroup> changelog = createJiraChangelogGroups(jiraIssue);
        issue.setChangelog(changelog);
    }

    private List<JiraChangelogGroup> createJiraChangelogGroups(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        List<JiraChangelogGroup> changelog = new ArrayList<>();
        if (jiraIssue.getChangelog() != null) {
            jiraIssue.getChangelog().forEach(changelogGroup -> changelog.add(createJiraChangelogGroup(changelogGroup)));
        }
        return changelog;
    }

    private JiraChangelogGroup createJiraChangelogGroup(ChangelogGroup changelogGroup) {
        final String noAuthor = "";
        String author = (changelogGroup.getAuthor() != null) ? changelogGroup.getAuthor().getName() : noAuthor;
        Date dateCreated = (changelogGroup.getCreated() != null) ? changelogGroup.getCreated().toDate() : new Date();
        List<JiraChangelogItem> changelogItems = createJiraChangelogItems(changelogGroup.getItems());
        return new JiraChangelogGroup(User.createWithUsername(author), dateCreated, changelogItems);
    }

    private List<JiraChangelogItem> createJiraChangelogItems(Iterable<ChangelogItem> changelogItems) {
        List<JiraChangelogItem> jiraChangelogItems = new ArrayList<>();
        if (changelogItems != null)
            changelogItems.forEach(item -> jiraChangelogItems.add(new JiraChangelogItem(item.getField(), item.getFrom(),
                    item.getFromString(), item.getTo(), item.getToString())));
        return jiraChangelogItems;
    }

    private void setResolution(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        if (jiraIssue.getResolution() != null && !"".equals(jiraIssue.getResolution())) {
            if (!JiraIssueResolution.hasId(jiraIssue.getResolution().getId())) {
                String msg = String.format("Could not convert issue resolution: %1$s (%2$s) for issue: %3$s", jiraIssue
                        .getResolution().getName(), jiraIssue.getResolution().getId(), jiraIssue.getKey());
                Utils.logWarnMessage(LOG, msg);
            } else
                issue.setResolution(JiraIssueResolution.getById(jiraIssue.getResolution().getId()));
        } else
            issue.setResolution(JiraIssueResolution.UNRESOLVED);

    }

    private static void setIssueAffectedVersions(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        if (jiraIssue.getAffectedVersions() != null) {
            List<String> affectedVersion = new ArrayList<String>(0);
            for (Version version : jiraIssue.getAffectedVersions())
                affectedVersion.add(version.getName());
            issue.setAffectedVersions(affectedVersion);
        }
    }

    // TODO find a solution for updating time estimates, see https://github.com/jboss-set/aphrodite/issues/23
    IssueInput issueToFluentUpdate(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, Project project) throws NotFoundException, AphroditeException {
        checkUnsupportedUpdateFields(issue);
        IssueInputBuilder inputBuilder = new IssueInputBuilder(jiraIssue.getProject().getKey(), jiraIssue.getIssueType().getId());


        issue.getSummary().ifPresent(inputBuilder::setSummary);
        inputBuilder.setFieldInput(new FieldInput(IssueFieldId.COMPONENTS_FIELD,
                issue.getComponents().stream().map(e -> ComplexIssueInputFieldValue.with("name", e)).collect(Collectors.toList()))
        );
        issue.getDescription().ifPresent(inputBuilder::setDescription);

        issue.getAssignee().ifPresent(assignee -> inputBuilder.setFieldInput(
                new FieldInput(IssueFieldId.ASSIGNEE_FIELD, ComplexIssueInputFieldValue.with("name",
                        assignee.getName().orElseThrow(this::nullUsername)))));

        // this is ok but does nothing if there is no permissions.
        issue.getStage().getStateMap().entrySet()
            .stream().filter(entry -> entry.getValue() != FlagStatus.NO_SET)
            .forEach(entry -> inputBuilder.setFieldInput(new FieldInput(JSON_CUSTOM_FIELD + FLAG_MAP.get(entry.getKey()), entry.getValue().getSymbol())));

        Map<String, Version> versionsMap = StreamSupport.stream(project.getVersions().spliterator(), false)
                .collect(Collectors.toMap(Version::getName, Function.identity()));
        updateFixVersions(issue, versionsMap, inputBuilder);
        updateStreamStatus(issue, jiraIssue, versionsMap, inputBuilder);

        if (!((JiraIssue)issue).getLabels().isEmpty()) {
            inputBuilder.setFieldValue("labels", ((JiraIssue) issue).getLabels().stream().map(JiraLabel::getName).collect(Collectors.toList()));
        }

        if (((JiraIssue)issue).isSecuritySensitiveIssue()) {
            inputBuilder.setFieldValue(JSON_CUSTOM_FIELD + SECURITY_SENSITIVE,
                    Arrays.asList(ComplexIssueInputFieldValue.with("id", SECURITY_SENSITIVE_VALUE_TRUE)));
        } else {
            inputBuilder.setFieldValue(JSON_CUSTOM_FIELD + SECURITY_SENSITIVE, Collections.emptyList());
        }
        if (((JiraIssue)issue).getSecurityLevel() != null) {
            String id = JiraFields.getSecurityLevelId(((JiraIssue)issue).getSecurityLevel());
            inputBuilder.setFieldValue(SECURITY_LEVEL, ComplexIssueInputFieldValue.with("id", id));
        }

        return inputBuilder.build();
    }

    private void updateStreamStatus(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue,
                                    Map<String, Version> versionsMap, IssueInputBuilder inputBuilder) throws NotFoundException {
        String customField = JSON_CUSTOM_FIELD + TARGET_RELEASE;
        IssueField issueField = jiraIssue.getField(customField);
        if (issueField == null) {
            String msg = String.format("Unable to set a stream status for issue %1$s as %2$s projects do not utilise field: %3$s",
                    jiraIssue.getKey(), jiraIssue.getProject().getName(), customField);
            Utils.logWarnMessage(LOG, msg);
            return;
        }

        for (Map.Entry<String, FlagStatus> entry : issue.getStreamStatus().entrySet()) {
            if (entry.getValue() != FlagStatus.ACCEPTED) {
                String streamName = entry.getKey();
                Version version = versionsMap.get(streamName);
                if (version != null) {
                    inputBuilder.setFieldValue(customField, ComplexIssueInputFieldValue.with("id", version.getId().toString()));
                } else {
                    throw new NotFoundException("No Stream exists for this project with the name : " + streamName);
                }
            }
        }
    }

    private void updateFixVersions(Issue issue, Map<String, Version> versionsMap, IssueInputBuilder inputBuilder) throws NotFoundException {
        List<Version> projectVersions = new ArrayList<>();
        for (Release release : issue.getReleases()) {
            String releaseName = release.getVersion().orElse(null);
            Version version = versionsMap.get(releaseName);
            if (version != null) {
                projectVersions.add(version);
            } else {
                throw new NotFoundException("No Release exists for this project with name : " + releaseName);
            }
        }
        inputBuilder.setFixVersions(projectVersions);
    }

    private void checkUnsupportedUpdateFields(Issue issue) {
        if (issue.getReporter().isPresent() && LOG.isDebugEnabled())
            LOG.debug("JIRA does not support updating the reporter field, field ignored.");
    }

    private void setIssueProject(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        BasicProject project = jiraIssue.getProject();
        if (project != null)
            issue.setProduct(project.getName());
    }

    private void setIssueComponent(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        Iterable<BasicComponent> components = jiraIssue.getComponents();
        List<String> tmp = new ArrayList<>();
        for(BasicComponent component : components) {
            tmp.add(component.getName());
        }
        issue.setComponents(tmp);
    }

    private IllegalArgumentException nullUsername() {
        throw new IllegalArgumentException("JIRA issues require a non-null username in order to set an assignee/reporter");
    }

    private void setIssueUser(BiConsumer<Issue, com.atlassian.jira.rest.client.api.domain.User> function, Issue issue,
                              com.atlassian.jira.rest.client.api.domain.User user) {
        if (user != null && user.getName() != null && user.getEmailAddress() != null)
            function.accept(issue, user);
    }

    private void setIssueStage(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        Stage stage = new Stage();
        setFlag(jiraIssue, stage, Flag.PM, JSON_CUSTOM_FIELD + PM_ACK);
        setFlag(jiraIssue, stage, Flag.DEV, JSON_CUSTOM_FIELD + DEV_ACK);
        setFlag(jiraIssue, stage, Flag.QE, JSON_CUSTOM_FIELD + QE_ACK);
        issue.setStage(stage);
    }

    private void setFlag(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, Stage stage, Flag flag, String fieldname) {
        if (jiraIssue.getField(fieldname) != null && jiraIssue.getField(fieldname).getValue() != null)
            stage.setStatus(flag, FlagStatus.getMatchingFlag((String) jiraIssue.getField(fieldname).getValue()));
        else {
            stage.setStatus(flag, FlagStatus.NO_SET);
        }
    }

    private void setIssueSprintRelease(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        final IssueField issueField = jiraIssue.getFieldByName("Sprint");
        if ( issueField != null && issueField.getValue() != null ) {
            JSONArray fieldValue = (JSONArray) issueField.getValue();
            if ( fieldValue.length() > 0 )
                issue.setSprintRelease(extractSprintName(fieldValue));
        }
    }

    /*
     * Horrible hack :( - but no other options apparently
     * See https://answers.atlassian.com/questions/92681/how-to-get-sprints-using-greenhopper-api
    */
    private static final String NAME_FIELD_ATTRIBUTE = "name=";
    private String extractSprintName(JSONArray fieldValue) {
        try {
            String value = (String) fieldValue.get(0);
            value = value.substring(value.indexOf(NAME_FIELD_ATTRIBUTE));
            return value.substring(NAME_FIELD_ATTRIBUTE.length(), value.indexOf(','));
        } catch ( JSONException e) {
            return "";
        }

    }

    private void setIssueStream(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        try {
            IssueField jsonField = jiraIssue.getField(JSON_CUSTOM_FIELD + TARGET_RELEASE);
            if (jsonField == null || jsonField.getValue() == null) {
                return;
            }
            JSONObject value = (JSONObject) jsonField.getValue();
            Map<String, FlagStatus> streamStatus;
            streamStatus = Collections.singletonMap(value.getString("name"), FlagStatus.ACCEPTED);
            issue.setStreamStatus(streamStatus);
        } catch (JSONException e) {
            LOG.error("error setting the stream in " + jiraIssue.getKey(), e);
        }

    }

    private void setIssueReleases(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        Iterable<Version> versions = jiraIssue.getFixVersions();
        if (versions != null) {
            List<Release> releases = StreamSupport.stream(jiraIssue.getFixVersions().spliterator(), false)
                    .map(version -> new Release(version.getName()))
                    .collect(Collectors.toList());
            issue.setReleases(releases);
        }
    }

    private void setIssueDependencies(URL originalUrl, Issue issue, Iterable<com.atlassian.jira.rest.client.api.domain.IssueLink> links) {
        if (links == null)
            return;
        final String INCORPORATES = "incorporates";

        for (com.atlassian.jira.rest.client.api.domain.IssueLink il : links) {
            // Add links of cloned to/from issues to the issue
            if (il.getIssueLinkType().getDescription().contains("cloned")) {
                URL url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                ((JiraIssue) issue).getLinkedCloneIssues().add(url);
            }

            // Add links of incorporates issues
            if(il.getIssueLinkType().getDescription().equals(INCORPORATES)) {
                URL url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                ((JiraIssue) issue).getLinkedIncorporatesIssues().add(url);
            }

            if (il.getIssueLinkType().getDirection().equals(Direction.INBOUND)) {
                URL url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                issue.getBlocks().add(url);
            } else {
                URL url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                issue.getDependsOn().add(url);
            }
        }
    }

    private void setIssueComments(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        List<Comment> comments = new ArrayList<>();
        jiraIssue.getComments()
                .forEach(c -> comments.add(new Comment(issue.getTrackerId().get(), Long.toString(c.getId()), c.getBody(), false)));
        issue.getComments().addAll(comments);
    }

    private void setPullRequests(JiraIssue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        IssueField fieldContent = jiraIssue.getFieldByName("Git Pull Request");//Git Pull Request
        if ( fieldContent != null ) {
            extractPullRequests(issue, (JSONArray) fieldContent.getValue());
        }
    }

    private static void extractPullRequests(JiraIssue issue, JSONArray urls) {
        if (urls != null && urls.length() > 0 ) {
            List<URL> prUrls = new ArrayList<URL>(urls.length());
            for ( int index = 0 ; index < urls.length(); index++ )
                prUrls.add(Utils.createURL(getFromJSONArray(index,urls).toString()));
            issue.setPullRequests(prUrls);
        }
    }

    private static Object getFromJSONArray(int i, JSONArray urls) {
        try {
            return urls.get(i);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
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
