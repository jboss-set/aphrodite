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

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
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

    private static final Integer DEFAULT_MAX_RESULTS = 50;
    private final SearchCriteria criteria;
    private final Map<String, Object> loginDetails;
    private Map<String, Object> queryMap;

    BugzillaQueryBuilder(SearchCriteria criteria, Map<String, Object> loginDetails) {
        this.criteria = criteria;
        this.loginDetails = loginDetails;
    }

    Map<String, Object> getQueryMap() {
        if (queryMap != null)
            return queryMap;

        queryMap = new HashMap<>(loginDetails);
        queryMap.put(RESULT_INCLUDE_FIELDS, RESULT_FIELDS);
        queryMap.put(RESULT_PERMISSIVE_SEARCH, true);
        queryMap.put(RESULT_LIMIT, criteria.getMaxResults().orElse(DEFAULT_MAX_RESULTS));

        criteria.getStatus().ifPresent(status -> queryMap.put(STATUS, status.toString()));
        criteria.getAssignee().ifPresent(assignee -> queryMap.put(ASSIGNEE, assignee));
        criteria.getLastUpdated().ifPresent(date -> queryMap.put(LAST_UPDATED, date.atStartOfDay().toString()));
        criteria.getProduct().ifPresent(product -> queryMap.put(PRODUCT, product));
        criteria.getComponent().ifPresent(component -> queryMap.put(COMPONENT, component));
        criteria.getRelease().ifPresent(release -> {
            release.getMilestone().ifPresent(milestone -> queryMap.put(TARGET_MILESTONE, milestone));
            release.getVersion().ifPresent(version -> queryMap.put(VERSION, version));
        });

        addStreamsAndStageToQueryMap();
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
}
