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

/**
 * @author wangc
 *
 */

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

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
import org.jboss.set.aphrodite.repository.services.common.RepositoryUtils;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

public class BugzillaPatchHomeImpl implements PatchHome {
    private static final Log logger = LogFactory.getLog(BugzillaPatchHomeImpl.class);

    @Override
    public Stream<Patch> findPatchesByIssue(Issue issue) {
        List<URL> urls = new ArrayList<>();
        issue.getComments().stream().forEach(e -> extractPullRequests(urls, e.getBody()));
        return mapURLtoPatchStream(urls);

    }

    private void extractPullRequests(List<URL> pullRequests, String messageBody) {
        Matcher matcher = RepositoryUtils.RELATED_PR_PATTERN.matcher(messageBody);
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                String urlStr = "https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pull/" + matcher.group(3);
                try {
                    URL url = new URL(urlStr);
                    pullRequests.add(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL:" + urlStr, e);
                }
            }
        }
    }

    private java.util.stream.Stream<Patch> mapURLtoPatchStream(List<URL> urls) {
        return urls.stream().map(e -> {
            PatchType patchType = getPatchType(e);
            PatchState patchState = getPatchState(e, patchType);
            return new Patch(e, getPatchType(e), patchState);
        });
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
                return PatchState.valueOf(pullRequest.getState().toString());
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
