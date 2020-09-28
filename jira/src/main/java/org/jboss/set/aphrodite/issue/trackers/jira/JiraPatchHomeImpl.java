/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.set.aphrodite.issue.trackers.jira;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.PatchType;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.spi.PatchHome;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author wangc
 *
 */
public class JiraPatchHomeImpl implements PatchHome {
    private static final Log logger = LogFactory.getLog(JiraPatchHomeImpl.class);

    @Override
    public java.util.stream.Stream<Patch> findPatchesByIssue(Issue issue) {
        List<URL> urls = ((JiraIssue) issue).getPullRequests();
        return mapURLtoPatchStream(urls);
    }

    private java.util.stream.Stream<Patch> mapURLtoPatchStream(List<URL> urls) {
        List<Patch> list = urls.stream().map(e -> {
            PatchType patchType = getPatchType(e);
            PatchState patchState = getPatchState(e, patchType);
            return new Patch(e, patchType, patchState);
            }).collect(Collectors.toList());
        return list.stream();
    }

    private PatchType getPatchType(URL url) {
        String urlStr = url.toString();
        if (urlStr.contains("/pull/"))
            return PatchType.PULLREQUEST;
        else if (urlStr.contains("/commit/"))
            return PatchType.COMMIT;
        else
            return PatchType.FILE;
    }

    private PatchState getPatchState(URL url, PatchType patchType) {
        if (patchType.equals(PatchType.PULLREQUEST)) {
            try {
                PullRequest pullRequest = Aphrodite.instance().getPullRequest(url);
                if (pullRequest !=null && pullRequest.getState() != null) {
                    return PatchState.valueOf(pullRequest.getState().toString());
                }
            } catch (NotFoundException e) {
                Utils.logException(logger, "Unable to find pull request with url: " + url, e);
            } catch (AphroditeException e) {
                Utils.logException(logger, e);
            }
        } else if (patchType.equals(PatchType.COMMIT)) {
            return PatchState.CLOSED;
        }
        return PatchState.UNDEFINED;
    }

}