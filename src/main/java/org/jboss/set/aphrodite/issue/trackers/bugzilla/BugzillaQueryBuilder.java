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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.domain.Stream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.*;

/**
 * @author Ryan Emerson
 */
class BugzillaQueryBuilder {

    private static final Log LOG = LogFactory.getLog(BugzillaQueryBuilder.class);

    private final SearchCriteria criteria;
    private final Map<String, Object> loginDetails;
    private final int defaultIssueLimit;
    private Map<String, Object> queryMap;

    BugzillaQueryBuilder(SearchCriteria criteria, Map<String, Object> loginDetails,
            int defaultIssueLimit) {
        this.criteria = criteria;
        this.loginDetails = loginDetails;
        this.defaultIssueLimit = defaultIssueLimit;
    }

    Map<String, Object> getQueryMap() {
        if (queryMap != null)
            return queryMap;

        queryMap = new HashMap<>();

        criteria.getAssignee().ifPresent(assignee -> queryMap.put(ASSIGNEE, assignee));
        criteria.getReporter().ifPresent(reporter -> queryMap.put(REPORTER, reporter));
        criteria.getLastUpdated().ifPresent(date -> queryMap.put(LAST_UPDATED, date.atStartOfDay().toString()));
        criteria.getProduct().ifPresent(product -> queryMap.put(PRODUCT, product));
        criteria.getComponent().ifPresent(component -> queryMap.put(COMPONENT, component));
        criteria.getRelease().ifPresent(release -> {
            release.getMilestone().ifPresent(milestone -> queryMap.put(TARGET_MILESTONE, milestone));
            release.getVersion().ifPresent(version -> queryMap.put(VERSION, version));
        });

        addStreamsAndStageToQueryMap();
        addIssueStatusToMap();

        // Necessary as an exception is thrown by BZ if queryMap is passed to the service without valid search fields
        if (queryMap.isEmpty()) {
            queryMap = null;
            return null;
        }
        queryMap.putAll(loginDetails);
        queryMap.put(RESULT_INCLUDE_FIELDS, RESULT_FIELDS);
        queryMap.put(RESULT_PERMISSIVE_SEARCH, true);

        int limit = criteria.getMaxResults().orElse(defaultIssueLimit);
        if (limit > 0)
            queryMap.put(RESULT_LIMIT, limit);

        return queryMap;
    }

    private void addStreamsAndStageToQueryMap() {
        Map<Stream, FlagStatus> streams = criteria.getStreams().orElse(Collections.emptyMap());
        if (streams.isEmpty())
            return;

        queryMap.put("j_top", "AND");
        int index = 1;
        for (Map.Entry<Stream, FlagStatus> entry : streams.entrySet()) {
            if (entry.getValue() == FlagStatus.NO_SET)
                continue;

            addFlagSearchToMap(index, entry.getKey().getName() + entry.getValue().getSymbol());
            index++;
        }

        if (!criteria.getStage().isPresent())
            return;

        Map<Flag, FlagStatus> stageMap = criteria.getStage().get().getStateMap();
        if (stageMap.isEmpty())
            return;

        for (Map.Entry<Flag, FlagStatus> entry : stageMap.entrySet()) {
            if (entry.getValue() == FlagStatus.NO_SET)
                continue;

            Optional<String> flag = getBugzillaFlag(entry.getKey());
            if (!flag.isPresent())
                continue;

            addFlagSearchToMap(index, flag.get() + entry.getValue().getSymbol());
            index++;
        }
    }

    private void addFlagSearchToMap(int index, String value) {
        queryMap.put(SEARCH_FUNCTION + index, SEARCH_FLAGS);
        queryMap.put(SEARCH_OPTION + index, SEARCH_EQUALS);
        queryMap.put(SEARCH_VALUE + index, value);
    }

    // IssueStatus.CREATED not supported by BZ, so ignore status and log message
    private void addIssueStatusToMap() {
        IssueStatus issueStatus = criteria.getStatus().orElse(IssueStatus.CREATED);
        if (issueStatus != IssueStatus.CREATED) {
            queryMap.put(STATUS, issueStatus.toString());
        } else {
            Utils.logWarnMessage(LOG, "Bugzilla issues do not support the IssueStatus CREATED, so this field is ignored "
                    + "when searching for issues");
        }
    }
}
