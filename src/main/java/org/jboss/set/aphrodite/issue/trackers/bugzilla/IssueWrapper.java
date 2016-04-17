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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaClient.ID_PARAM_PATTERN;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ASSIGNEE;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.BLOCKS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMPONENT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.CREATION_TIME;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.DEPENDS_ON;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.DESCRIPTION;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ESTIMATED_TIME;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.FLAGS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.FLAG_NAME;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.FLAG_STATUS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.HOURS_WORKED;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ID;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ID_QUERY;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ISSUE_IDS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ISSUE_TYPE;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.LAST_UPDATED;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_SET_COLLECTION;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.PRODUCT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.REPORTER;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.STATUS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.SUMMARY;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.TARGET_MILESTONE;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.VERSION;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.getAphroditeFlag;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.getBugzillaFlag;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.domain.User;
import org.jboss.set.aphrodite.spi.AphroditeException;

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    private static final Log LOG = LogFactory.getLog(BugzillaIssueTracker.class);

    Issue bugzillaBugToIssue(Map<String, Object> bug, URL baseURL) {
        Integer id = (Integer) bug.get(ID);
        URL url = Utils.createURL(baseURL + ID_QUERY + id);
        Issue issue = new Issue(url, TrackerType.BUGZILLA);
        issue.setTrackerId(id.toString());
        issue.setAssignee(User.createWithEmail((String) bug.get(ASSIGNEE)));
        issue.setReporter(User.createWithEmail((String) bug.get(REPORTER)));
        issue.setCreationTime((Date) bug.get(CREATION_TIME));
        issue.setLastUpdated((Date) bug.get(LAST_UPDATED));
        issue.setSummary((String) bug.get(SUMMARY));
        issue.setDescription((String) bug.get(DESCRIPTION));
        issue.setStatus(IssueStatus.valueOf((String) bug.get(STATUS)));
        Object[] components = (Object[]) bug.get(COMPONENT);
        List<String> tmp = new ArrayList<>();
        for(Object component : components) {
            tmp.add((String) component);
        }
        issue.setComponents(tmp);
        issue.setProduct((String) bug.get(PRODUCT));
        issue.setStatus(IssueStatus.valueOf(((String) bug.get(STATUS)).toUpperCase()));

        String type = (String) bug.get(ISSUE_TYPE);
        try {
            issue.setType(IssueType.valueOf(type.replaceAll("\\s+","_").toUpperCase()));
        } catch (IllegalArgumentException e) {
            issue.setType(IssueType.UNDEFINED);
        }

        String version = (String) ((Object[]) bug.get(VERSION))[0];
        List<Release> releases = new ArrayList<>();
        releases.add(new Release(version, (String) bug.get(TARGET_MILESTONE)));
        issue.setReleases(releases);

        issue.setDependsOn(getListOfURlsFromIds(bug, baseURL, DEPENDS_ON));
        issue.setBlocks(getListOfURlsFromIds(bug, baseURL, BLOCKS));

        checkIsNullEstimation(bug,issue);
        extractStageAndStreams(bug, issue);
        return issue;
    }

    private void checkIsNullEstimation(Map<String, Object> bug, Issue issue) {
        Double estimatedTime = (Double) bug.get(ESTIMATED_TIME);
        Double hoursWorked = (Double) bug.get(HOURS_WORKED);
        if (estimatedTime != null && hoursWorked != null) {
            issue.setEstimation(new IssueEstimation(estimatedTime, hoursWorked));
        } else if (estimatedTime != null) {
            issue.setEstimation(new IssueEstimation(estimatedTime));
        }

    }

    Map<String, Object> issueToBugzillaBug(Issue issue, Map<String, Object> loginDetails) throws AphroditeException {
        checkUnsupportedUpdateFields(issue);
        checkUnsupportedIssueStatus(issue);

        Map<String, Object> params = new HashMap<>(loginDetails);
        issue.getTrackerId().ifPresent(trackerId -> params.put(ISSUE_IDS, trackerId));
        issue.getSummary().ifPresent(summary -> params.put(SUMMARY, summary));
        issue.getProduct().ifPresent(product -> params.put(PRODUCT, product));
        params.put(COMPONENT, issue.getComponents().toArray(new String[issue.getComponents().size()]));
        issue.getAssignee().ifPresent(assignee -> params.put(ASSIGNEE, assignee.getEmail().orElseThrow(this::nullUserEmail)));
        issue.getReporter().ifPresent(reporter -> params.put(REPORTER, reporter.getEmail().orElseThrow(this::nullUserEmail)));

        issue.getEstimation().ifPresent(tracking -> {
            params.put(HOURS_WORKED, tracking.getHoursWorked());
            params.put(ESTIMATED_TIME, tracking.getInitialEstimate());
        });

        params.put(STATUS, issue.getStatus().toString());
        params.put(FLAGS, getStageAndStreamsMap(issue.getStreamStatus(), issue.getStage().getStateMap()));

        if (issue.getType() != IssueType.UNDEFINED)
            params.put(ISSUE_TYPE, issue.getType().toString());

        addReleaseToUpdate(issue, params);
        addURLCollectionToParameters(issue.getDependsOn(), DEPENDS_ON, params);
        addURLCollectionToParameters(issue.getBlocks(), BLOCKS, params);
        return params;
    }

    private IllegalArgumentException nullUserEmail() {
        throw new IllegalArgumentException("Bugzilla requires a non-null User.email field to update assignee/reporters");
    }

    private void checkUnsupportedUpdateFields(Issue issue) {
        if (issue.getReporter().isPresent() && LOG.isWarnEnabled())
            LOG.warn("Bugzilla does not support updating the reporter field, field ignored.");

        if (issue.getStatus() == IssueStatus.UNDEFINED && LOG.isWarnEnabled())
            LOG.warn("IssueStatus.UNDEFINED is ignored when updating a Bugzilla operation.");
    }

    private void checkUnsupportedIssueStatus(Issue issue) throws AphroditeException {
        if (issue.getStatus() == IssueStatus.CREATED) {
            throw new AphroditeException("Bugzilla issues do not support the IssueStatus CREATED");
        }
    }

    private void addReleaseToUpdate(Issue issue, Map<String, Object> params) {
        List<Release> releases = issue.getReleases();
        if (!releases.isEmpty()) {
            Release release = releases.get(0);
            release.getVersion().ifPresent(version -> params.put(VERSION, version));
            release.getMilestone().ifPresent(milestone -> params.put(TARGET_MILESTONE, milestone));

            if (releases.size() > 1) {
                Utils.logWarnMessage(LOG, "Bugzilla only supports one Release object. The first Release object in " +
                        "Issue::getReleases is used and subsequent Releases are ignored.");
            }
        }
    }

    private List<Map<String, Object>> getStageAndStreamsMap(Map<String, FlagStatus> streams, Map<Flag, FlagStatus> stateMap) {
        List<Map<String, Object>> flags = new ArrayList<>();
        for (Map.Entry<String, FlagStatus> stream : streams.entrySet()) {
            Map<String, Object> flagMap = new HashMap<>();
            flagMap.put(FLAG_NAME, stream.getKey());
            flagMap.put(FLAG_STATUS, stream.getValue().getSymbol());
            flags.add(flagMap);
        }

        for (Map.Entry<Flag, FlagStatus> entry : stateMap.entrySet()) {
            Map<String, Object> flagMap = new HashMap<>();
            Optional<String> bzFlag = getBugzillaFlag(entry.getKey());
            bzFlag.ifPresent(flagName -> {
                flagMap.put(FLAG_NAME, flagName);
                flagMap.put(FLAG_STATUS, entry.getValue().getSymbol());
                flags.add(flagMap);
            });
        }
        return flags;
    }

    private void addURLCollectionToParameters(List<URL> urls, String flag, Map<String, Object> params) {
        Map<String, Object> map = new HashMap<>();
        List<String> ids = Utils.getParametersFromUrls(ID_PARAM_PATTERN, urls);
        map.put(METHOD_SET_COLLECTION, ids);
        params.put(flag, map);
    }

    private List<URL> getListOfURlsFromIds(Map<String, Object> bug, URL baseURL, String field) {
        List<URL> list = new ArrayList<>();
        Object[] ids = (Object[]) bug.get(field);
        for (Object id : ids)
            list.add(Utils.createURL(baseURL + BugzillaFields.ID_QUERY + id));
        return list;
    }

    private void extractStageAndStreams(Map<String, Object> bug, Issue issue) {
        Stage issueStage = new Stage();
        Map<String, FlagStatus> streams = new HashMap<>();
        if (bug.get(FLAGS) != null) {
            for (Object object : (Object[]) bug.get(FLAGS)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> flagMap = (Map<String, Object>) object;
                String name = (String) flagMap.get(FLAG_NAME);

                if (name.contains("_ack")) { // If Flag
                    Optional<Flag> flag = getAphroditeFlag(name);
                    if (!flag.isPresent())
                        continue;

                    FlagStatus status = FlagStatus.getMatchingFlag((String) flagMap.get(FLAG_STATUS));
                    issueStage.setStatus(flag.get(), status);
                } else { // Else Stream
                    FlagStatus status = FlagStatus.getMatchingFlag((String) flagMap.get(FLAG_STATUS));
                    streams.put(name, status);
                }
            }
        }
        issue.setStage(ensureStageMapIsComplete(issueStage));
        issue.setStreamStatus(streams);
    }

    // Ensure all missing flag, if any are set to NO_SET
    private static Stage ensureStageMapIsComplete(Stage issue) {
        for (Flag flag : Flag.values())
            issue.getStateMap().putIfAbsent(flag, FlagStatus.NO_SET);

        return issue;
    }
}
