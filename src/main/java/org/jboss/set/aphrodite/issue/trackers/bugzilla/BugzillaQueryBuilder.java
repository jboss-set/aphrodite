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
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.spi.SearchCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        if (criteria.getLastUpdated().isPresent())
            queryMap.put(LAST_UPDATED, criteria.getLastUpdated().get().atStartOfDay().toString());

        addOptionalToMap(PRODUCT, criteria.getProduct(), queryMap);
        addOptionalToMap(COMPONENT, criteria.getComponent(), queryMap);

        if (criteria.getRelease().isPresent()) {
            Release release = criteria.getRelease().get();
            addOptionalToMap(TARGET_MILESTONE, release.getMilestone(), queryMap);
            addOptionalToMap(VERSION, release.getVersion(), queryMap);
        }

        // TODO, this does not currently work! Appears that BZ does not support flag queries via XMLRPC
        List<Map<String, Object>> flags = new ArrayList<>();
        addStageFlagsToSearchQuery(flags);
        addStreamsToSearchQuery(flags);
        if (!flags.isEmpty())
            queryMap.put(FLAGS, flags.toArray());

        return queryMap;
    }

    private void addStreamsToSearchQuery(List<Map<String, Object>> flags) {
        if (criteria.getStreams().isPresent()) {
            List<Stream> streams = criteria.getStreams().get();
            for (Stream stream : streams) {
                Map<String, Object> flagMap = new HashMap<>();
                flagMap.put(FLAG_NAME, stream.getName());
                flagMap.put(FLAG_STATUS, stream.getStatus().getSymbol());
                flags.add(flagMap);
            }
        }
    }

    private void addStageFlagsToSearchQuery(List<Map<String, Object>> flags) {
        if (criteria.getStage().isPresent()) {
            Stage stage = criteria.getStage().get();
            for (Map.Entry<Flag, FlagStatus> entry : stage.getStateMap().entrySet()) {
                if (entry.getValue() == FlagStatus.NO_SET)
                    continue;

                Map<String, Object> flagMap = new HashMap<>();
                Optional<String> flag = getBugzillaFlag(entry.getKey());
                if (flag.isPresent()) {
                    flagMap.put(FLAG_NAME, flag.get());
                    flagMap.put(FLAG_STATUS, entry.getValue().getSymbol());
                    flags.add(flagMap);
                }
            }
        }
    }

    private void addOptionalToMap(String key, Optional optional, Map<String, Object> map) {
        if (optional.isPresent())
            map.put(key, optional.get());
    }
}
