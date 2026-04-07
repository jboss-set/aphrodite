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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import org.jboss.set.aphrodite.issue.trackers.common.AbstractIssueTracker;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
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

import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.BROWSE_ISSUE_PATH;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.DEV_ACK;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.FLAG_MAP;
import static org.jboss.set.aphrodite.issue.trackers.jira.JiraFields.INVOLVED_FIELD;
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

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(JiraIssueTracker.class);

    Issue jiraSearchIssueToIssue(URI baseURL, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        URI url = trackerIdToBrowsableUrl(baseURL, jiraIssue.getKey());
        return jiraIssueToIssue(url, jiraIssue);
    }

    private void setCreationTime(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        issue.setCreationTime(jiraIssue.getCreationDate().toDate());
    }

    private void setLastUpdated(Issue issue, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        issue.setLastUpdated(jiraIssue.getUpdateDate().toDate());
    }

    Issue jiraIssueToIssue(URI url, com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
        JiraIssue issue = new JiraIssue(url);
        copy(url, jiraIssue, issue);
        return issue;
    }

    void copy(final URI url, final com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, final JiraIssue issue) {
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

        setIssueUser((i, u) -> i.setAssignee(createUserFromJiraUser(u)), issue, jiraIssue.getAssignee());
        setIssueUser((i, u) -> i.setReporter(createUserFromJiraUser(u)), issue, jiraIssue.getReporter());

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
        setInvolved(jiraIssue, issue);
    }

    private void setInvolved(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, JiraIssue issue) {
        ArrayList<String> involved = new ArrayList<>();
        IssueField invField = jiraIssue.getField(JSON_CUSTOM_FIELD + JiraFields.INVOLVED_FIELD);
        if (invField != null && invField.getValue() != null) {
            JSONArray invArray = (JSONArray) jiraIssue.getField(JSON_CUSTOM_FIELD + JiraFields.INVOLVED_FIELD).getValue();
            for (int i = 0; i < invArray.length(); i++) {
                try {
                    involved.add(((JSONObject) invArray.get(i)).getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        issue.setInvolved(involved);
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
        String author = "";
        BasicUser user = changelogGroup.getAuthor();
        if (user != null && user.getName() != null) {
            author = user.getName();
        } else if (user != null && user.getAccountId() != null) {
            author = user.getAccountId();
        }
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

        issue.getAssignee().ifPresent(assignee -> {
            // JIRA Cloud uses accountId, JIRA Server uses name. Try accountId first (Cloud), fallback to name (Server)
            if (assignee.getAccountId().isPresent()) {
                inputBuilder.setFieldInput(
                    new FieldInput(IssueFieldId.ASSIGNEE_FIELD,
                        ComplexIssueInputFieldValue.with("accountId", assignee.getAccountId().get())));
            } else if (assignee.getName().isPresent()) {
                inputBuilder.setFieldInput(
                    new FieldInput(IssueFieldId.ASSIGNEE_FIELD,
                        ComplexIssueInputFieldValue.with("name", assignee.getName().get())));
            } else {
                throw nullUsername();
            }
        });

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

        if (((JiraIssue)issue).getSecurityLevel().isPresent()) {
            String id = JiraFields.getSecurityLevelId(((JiraIssue)issue).getSecurityLevel().get());
            inputBuilder.setFieldValue(SECURITY_LEVEL, ComplexIssueInputFieldValue.with("id", id));
        }

        if (!((JiraIssue)issue).getInvolved().isEmpty()) {
            ArrayList<ComplexIssueInputFieldValue> inv = new ArrayList<>();
            for (String name : ((JiraIssue) issue).getInvolved()) {
                inv.add(ComplexIssueInputFieldValue.with("name", name));
            }
            inputBuilder.setFieldValue(JSON_CUSTOM_FIELD + INVOLVED_FIELD, inv);
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

    /**
     * Create Aphrodite User from JIRA REST client User object.
     * Handles both JIRA Server (uses getName()) and JIRA Cloud (uses getAccountId()).
     */
    private User createUserFromJiraUser(com.atlassian.jira.rest.client.api.domain.User jiraUser) {
        String email = jiraUser.getEmailAddress();
        String name = jiraUser.getName();
        String accountId = null;
        try {
            // AccountId is part of the self URI in Cloud: /rest/api/2/user?accountId=xxx
            String self = jiraUser.getSelf() != null ? jiraUser.getSelf().toString() : null;
            if (self != null && self.contains("accountId=")) {
                accountId = self.substring(self.indexOf("accountId=") + "accountId=".length());
                // Remove any trailing query parameters
                if (accountId.contains("&")) {
                    accountId = accountId.substring(0, accountId.indexOf("&"));
                }
            }
        } catch (Exception e) {
            // If accountId extraction fails, fall back to name
        }
        return new User(email, name, accountId);
    }

    private IllegalArgumentException nullUsername() {
        throw new IllegalArgumentException("JIRA issues require a non-null username in order to set an assignee/reporter");
    }

    private void setIssueUser(BiConsumer<Issue, com.atlassian.jira.rest.client.api.domain.User> function, Issue issue,
                              com.atlassian.jira.rest.client.api.domain.User user) {
        // Accept user if it has email and either name (Server) or accountId (Cloud)
        if (user != null && user.getEmailAddress() != null &&
            (user.getName() != null || user.getSelf() != null))
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

    private void setIssueDependencies(URI originalUrl, Issue issue, Iterable<com.atlassian.jira.rest.client.api.domain.IssueLink> links) {
        if (links == null)
            return;
        final String INCORPORATES = "incorporates";

        for (com.atlassian.jira.rest.client.api.domain.IssueLink il : links) {
            // Add links of cloned to/from issues to the issue
            if (il.getIssueLinkType().getDescription().contains("cloned")
                    || il.getIssueLinkType().getDescription().contains("clones")) {
                URI url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                ((JiraIssue) issue).getLinkedCloneIssues().add(url);
            }

            // Add links of incorporates issues
            if(il.getIssueLinkType().getDescription().equals(INCORPORATES)) {
                URI url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                ((JiraIssue) issue).getLinkedIncorporatesIssues().add(url);
            }

            if (il.getIssueLinkType().getDirection().equals(Direction.INBOUND)) {
                URI url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
                issue.getBlocks().add(url);
            } else {
                URI url = trackerIdToBrowsableUrl(originalUrl, il.getTargetIssueKey());
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
        if (fieldContent == null ) {
            return;
        }

        String uris = (String) fieldContent.getValue();

        if (uris == null || uris.isEmpty()) {
            return;
        }
        issue.setPullRequests(extractUris(uris));
    }

    private List<URI> extractUris(String input) {
        Set<URI> uris = new TreeSet<>();
        Pattern pattern = Pattern.compile(AbstractIssueTracker.REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            try {
                // Convert the match to an actual URI object for validation
                uris.add(new URI(matcher.group()));
            } catch (Exception e) {
                // Ignore matches that aren't valid URIs
            }
        }
        return new ArrayList<>(uris);
    }

    private URI trackerIdToBrowsableUrl(URI url, String trackerId) {
        try {
            String link = url.getScheme() + "://" + url.getHost() + BROWSE_ISSUE_PATH + trackerId;
            return new URI(link);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
