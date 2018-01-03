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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ID_QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Payload;
import org.jboss.set.aphrodite.domain.spi.PayloadHome;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

public class BugzillaPayloadHome implements PayloadHome {

    private static final Logger logger = Logger.getLogger(BugzillaPayloadHome.class.getCanonicalName());

    private static final String BUGZILLA_HOST = "https://bugzilla.redhat.com/";

    @Override
    public Payload findPayload(String name) {
        if (!name.startsWith("eap64") || !name.endsWith("-payload")) {
            logger.log(Level.WARNING, "Incorrect bugzilla payload alias name format " + name);
        }
        // URL with alias format https://bugzilla.redhat.com/show_bug.cgi?id=eap6420-payload
        String url = BUGZILLA_HOST + ID_QUERY + name;
        try {
            Issue issue = Aphrodite.instance().getIssue(new URL(url));
            return new BugzillaPayload(issue);
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Unable to form URL from " + url, e);
        } catch (NotFoundException e) {
            logger.log(Level.WARNING, "Unable to find bugzilla payload parent tracker issue from " + url, e);
        } catch (AphroditeException e) {
            logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
        }
        return null;
    }

    @Override
    public List<Payload> findAllPayloads() {
        // TODO how to find all Bugzilla payload by alias (ap6420-payload) ?
        throw new RuntimeException("NYI: org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaPayloadHome.findAllPayloads");
    }
}
