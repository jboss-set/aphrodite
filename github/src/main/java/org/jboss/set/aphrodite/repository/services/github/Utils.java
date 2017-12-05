/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.repository.services.github;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;

/**
 * Github specific utilities methods
 *
 * @author wangc
 *
 */
public class Utils {

    public static GHCommitState getCombineStatus(List<GHCommitStatus> comStatuses) {
        int count = 0, flag = 0;
        List<GHCommitState> stas = new ArrayList<>();
        for (GHCommitStatus status : comStatuses) {
            GHCommitState sta = status.getState();
            stas.add(sta);
            // until sta="pending"
            if (!sta.equals(GHCommitState.PENDING)) {
                if (sta.equals(GHCommitState.FAILURE)) {
                    return GHCommitState.FAILURE;
                } else if (sta.equals(GHCommitState.ERROR)) {
                    return GHCommitState.ERROR;
                }
            } else {
                flag = 1;
                // The Travis CI and TeamCity Build has different rules
                String description = status.getDescription();
                if (description != null && description.contains("Travis")) {
                    return stas.contains(GHCommitState.SUCCESS) ? GHCommitState.SUCCESS : GHCommitState.PENDING;
                }
                if (comStatuses.size() > 2 * count) {
                    GHCommitState temp = comStatuses.get(2 * count).getState();
                    return temp.equals(GHCommitState.PENDING) ? GHCommitState.PENDING : GHCommitState.SUCCESS;
                } else if (comStatuses.size() == 2 * count) {
                    return GHCommitState.SUCCESS;
                }
            }
            count++;
        }

        return (flag == 0) ? GHCommitState.SUCCESS : null;
    }

}
