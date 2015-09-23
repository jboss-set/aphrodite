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

import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.spi.SearchCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        criteria.getLastUpdated().ifPresent(date -> queryMap.put(LAST_UPDATED, date.atStartOfDay().toString()));
        criteria.getProduct().ifPresent(product -> queryMap.put(PRODUCT, product));
        criteria.getComponent().ifPresent(component -> queryMap.put(COMPONENT, component));
        criteria.getRelease().ifPresent(release -> {
            release.getMilestone().ifPresent(milestone -> queryMap.put(TARGET_MILESTONE, milestone));
            release.getVersion().ifPresent(version -> queryMap.put(VERSION, version));
        });

        // TODO, this does not currently work! Appears that BZ does not support flag queries via XMLRPC
        List<Map<String, Object>> flags = new ArrayList<>();
        addStageFlagsToSearchQuery(flags);
        addStreamsToSearchQuery(flags);
        if (!flags.isEmpty())
            queryMap.put(FLAGS, flags.toArray());

        return queryMap;
    }

    private void addStreamsToSearchQuery(List<Map<String, Object>> flags) {
        criteria.getStreams().ifPresent(streams -> streams
                .forEach(stream -> {
                    Map<String, Object> flagMap = new HashMap<>();
                    flagMap.put(FLAG_NAME, stream.getName());
                    flagMap.put(FLAG_STATUS, stream.getStatus().getSymbol());
                    flags.add(flagMap);
                }));
    }

    private void addStageFlagsToSearchQuery(List<Map<String, Object>> flags) {
        criteria.getStage().ifPresent(
                stage -> stage.getStateMap().entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != FlagStatus.NO_SET)
                        .forEach(entry -> getBugzillaFlag(entry.getKey()).ifPresent(
                                flag -> {
                                    Map<String, Object> flagMap = new HashMap<>();
                                    flagMap.put(FLAG_NAME, flag);
                                    flagMap.put(FLAG_STATUS, entry.getValue().getSymbol());
                                    flags.add(flagMap);
                                })));
    }
}
