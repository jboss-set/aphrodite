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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.jboss.set.aphrodite.container.Container;
import org.jboss.set.aphrodite.domain.Payload;
import org.jboss.set.aphrodite.domain.spi.PayloadHome;

import com.atlassian.jira.rest.client.api.domain.Version;

/**
 * @author wangc
 *
 */
public class JiraPayloadHome implements PayloadHome {

    private static final Logger logger = Logger.getLogger(JiraPayloadHome.class.getCanonicalName());
    private static final String PROJECT_NAME = "JBEAP";
    private static final Pattern PAYLOAD_VERSION = Pattern.compile("[7-9].[0-9].[0-9]*.GA");

    @Override
    public Payload findPayload(String name) {
        if (!PAYLOAD_VERSION.matcher(name).find()) {
            logger.log(Level.WARNING, "Incorrect jira payload name " + name);
        }

        Version version;
        try {
            version = Container.instance().lookup(JiraIssueTracker.class.getSimpleName(), JiraIssueTracker.class).getVersionByName(PROJECT_NAME, name);
            return new JiraPayload(version);
        } catch (NameNotFoundException e) {
            logger.log(Level.WARNING, "Missing service! It's unable to find payload by name " + name, e);
        }
        return null;
    }

    @Override
    public List<Payload> findAllPayloads() {
        List<Payload> payloads = new ArrayList<>();
        try {
            List<Version> versions = Container.instance().lookup(JiraIssueTracker.class.getSimpleName(), JiraIssueTracker.class).getVersionsByProject(PROJECT_NAME);
            payloads = versions.stream()
                    .filter(v -> filterPayloadVersion(v))
                    .map(v -> new JiraPayload(v))
                    .collect(Collectors.toList());
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Missing JiraIssueTracker service! It's unable to find payloads for project " + PROJECT_NAME, e);
        }
        return payloads;
    }

    // Examples: "6.1.0.Alpha1", "6.1.0.CR1", "6.1.0.GA", "6.1.1.GA", "6.2.0.GA", "6.3.0.GA", "6.4.0.GA", "7.0.0.DR12",
    // "7.0.0.DR13 (Alpha)", "7.0.0.ER2 (Beta)", "7.0.0.ER7", "7.0.0.CR2", "7.0.0.GA", "7.0.3.CR3-doc","7.0.8.CR1","7.0.7.CR3",
    // "7.0.7.CR1", "7.0.7.CR2", "7.0.9.GA", "7.0.10.GA", "7.1.0.DR19", "7.1.0.ER3","7.1.0.CR4", "7.1.0.GA", "7.0.z.GA",
    // "7.1.1.GA", "7.2.0.GA", "7.1.z.GA", "7.Doc.Test", "Individual Patches.GA", "7.backlog.GA"
    protected static boolean filterPayloadVersion(Version version) {
        if (PAYLOAD_VERSION.matcher(version.getName()).find()) {
            return true;
        } else {
            return false;
        }
    }
}
