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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.Payload;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author wangc
 *
 */
public class BugzillaPayload implements Payload {

    private static final Logger logger = Logger.getLogger(BugzillaPayload.class.getCanonicalName());

    private final Issue issue;

    BugzillaPayload(final Issue issue) {
        this.issue = issue;
    }

    @Override
    public String getName() {
        return issue.getSummary().orElse("N/A");
    }

    @Override
    public URL getUrl() {
        return issue.getURL();
    }

    @Override
    public List<? extends Issue> getIssues() {
        List<Issue> issues = new ArrayList<>();
        for (URL url : issue.getDependsOn()) {
            try {
                Issue issue = Aphrodite.instance().getIssue(url);
                issues.add(issue);
            } catch (NotFoundException e) {
                logger.log(Level.WARNING, "No issue found from url: " + url);
            } catch (AphroditeException e) {
                logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
            }
        }
        return issues;
    }

    @Override
    public boolean isReleased() {
        if ((issue.getStatus().equals(IssueStatus.CLOSED) || issue.getStatus().equals(IssueStatus.VERIFIED))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getSize() {
        return this.issue.getDependsOn().size();
    }
}
