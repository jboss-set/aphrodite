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

import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.domain.Stream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaClient.ID_PARAM_PATTERN;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.*;

/**
 * @author Ryan Emerson
 */
class IssueWrapper {

    Issue bugzillaBugToIssue(Map<String, Object> bug, URL baseURL) throws MalformedURLException {
        Integer id = (Integer) bug.get(ID);
        URL url = new URL(baseURL + ID_QUERY + id);
        Issue issue = new Issue(url);
        issue.setTrackerId(id.toString());
        issue.setAssignee((String) bug.get(ASSIGNEE));
        issue.setDescription((String) bug.get(DESCRIPTION));
        issue.setStatus(IssueStatus.valueOf((String) bug.get(STATUS)));
        issue.setComponent((String) ((Object[]) bug.get(COMPONENT))[0]);
        issue.setProduct((String) bug.get(PRODUCT));
        issue.setStatus(IssueStatus.valueOf(((String) bug.get(STATUS)).toUpperCase()));

        String type = (String) bug.get(ISSUE_TYPE);
        try {
            issue.setType(IssueType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException e) {
            issue.setType(IssueType.UNDEFINED);
        }

        String version = (String) ((Object[]) bug.get(VERSION))[0];
        Release release = new Release(version, (String) bug.get(TARGET_MILESTONE));
        issue.setRelease(release);

        issue.setDependsOn(getListOfURlsFromIds(bug, baseURL, DEPENDS_ON));
        issue.setBlocks(getListOfURlsFromIds(bug, baseURL, BLOCKS));

        Double estimatedTime = (Double) bug.get(ESTIMATED_TIME);
        Double hoursWorked = (Double) bug.get(HOURS_WORKED);
        issue.setEstimation(new IssueEstimation(estimatedTime, hoursWorked));

        extractStageAndStreams(bug, issue);
        return issue;
    }

    Map<String, Object> issueToBugzillaBug(Issue issue, Map<String, Object> loginDetails) {
        Map<String, Object> params = new HashMap<>(loginDetails);
        issue.getTrackerId().ifPresent(trackerId -> params.put(ISSUE_IDS, trackerId));
        issue.getProduct().ifPresent(product -> params.put(PRODUCT, product));
        issue.getComponent().ifPresent(component -> params.put(COMPONENT, component));
        issue.getAssignee().ifPresent(assignee -> params.put(ASSIGNEE, assignee));
        issue.getRelease().getVersion().ifPresent(version -> params.put(VERSION, version));
        issue.getRelease().getMilestone().ifPresent(milestone -> params.put(TARGET_MILESTONE, milestone));
        issue.getEstimation().ifPresent(tracking -> {
            params.put(HOURS_WORKED, tracking.getHoursWorked());
            params.put(ESTIMATED_TIME, tracking.getInitialEstimate());
        });

        params.put(STATUS, issue.getStatus().toString());
        params.put(ISSUE_TYPE, issue.getType().toString());
        params.put(FLAGS, getStageAndStreamsMap(issue.getStreams(), issue.getStage().getStateMap()));

        addURLCollectionToParameters(issue.getDependsOn(), DEPENDS_ON, params);
        addURLCollectionToParameters(issue.getBlocks(), BLOCKS, params);

        return params;
    }

    private List<Map<String, Object>> getStageAndStreamsMap(List<Stream> streams, Map<Flag, FlagStatus> stateMap) {
        List<Map<String, Object>> flags = new ArrayList<>();
        for (Stream stream : streams) {
            Map<String, Object> flagMap = new HashMap<>();
            flagMap.put(FLAG_NAME, stream.getName());
            flagMap.put(FLAG_STATUS, stream.getStatus().getSymbol());
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
        List<String> ids = Utils.getTrackerIdsFromUrls(ID_PARAM_PATTERN, urls);
        map.put(METHOD_SET_COLLECTION, ids);
        params.put(flag, map);
    }

    private List<URL> getListOfURlsFromIds(Map<String, Object> bug, URL baseURL, String field) throws MalformedURLException {
        List<URL> list = new ArrayList<>();
        Object[] ids = (Object[]) bug.get(field);
        for (Object id : ids)
            list.add(new URL(baseURL + BugzillaFields.ID_QUERY + id));
        return list;
    }

    private void extractStageAndStreams(Map<String, Object> bug, Issue issue) {
        Stage issueStage = new Stage();
        List<Stream> streams = new ArrayList<>();
        for (Object object : (Object[]) bug.get(FLAGS)) {
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
                streams.add(new Stream(name, status));
            }
        }
        issue.setStage(issueStage);
        issue.setStreams(streams);
    }
}
