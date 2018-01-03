/*
 * Copyright 2018 Red Hat, Inc.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Payload;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.spi.AphroditeException;

import com.atlassian.jira.rest.client.api.domain.Version;

/**
 * @author wangc
 *
 */
public class JiraPayload implements Payload {

    private static final boolean devProfile = System.getProperty("dev") != null;

    private static final Logger logger = Logger.getLogger(JiraPayload.class.getCanonicalName());

    private final Version version;
    private URL url;
    private List<? extends Issue> issues;

    JiraPayload(Version version) {
        this.version = version;
    }

    @Override
    public String getName() {
        return version.getName();
    }

    @Override
    public URL getUrl() {
        try {
            this.url = version.getSelf().toURL();
            return url;
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Unable to retrieve payload url from " + version.getSelf(), e);
        }
        return null;
    }

    @Override
    public List<? extends Issue> getIssues() {
        int maxResults = devProfile ? 10 : 200;
        SearchCriteria sc = new SearchCriteria.Builder()
                .setRelease(new Release(version.getName().trim()))
                .setProduct("JBEAP")
                .setMaxResults(maxResults)
                .build();
        try {
            issues = Aphrodite.instance().searchIssues(sc);
        } catch (AphroditeException e) {
            issues = Collections.emptyList();
            logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
        }
        return issues;
    }

    @Override
    public int getSize() {
        if (issues == null) {
            getIssues();
        }
        return getIssues().size();
    }

    @Override
    public boolean isReleased() {
        return version.isReleased();
    }

    public Version getVersion() {
        return version;
    }

}
