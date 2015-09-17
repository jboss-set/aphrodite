/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared.connectors.jira;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.atlassian.jira.rest.client.domain.Field;
import com.atlassian.jira.rest.client.domain.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.common.Flag;
import org.jboss.pull.shared.connectors.common.Issue;

/**
 * JIRA issue representation.
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class JiraIssue implements Issue {

    private static Log LOG = LogFactory.getLog(JiraIssue.class);

    private URL url;

    public enum IssueStatus {
        NEW, CODING_IN_PROGRESS, OPEN, RESOLVED, READY_FOR_QA, PULL_REQUEST_SENT, QA_IN_PROGRESS, VERIFIED, REOPENED,
        CLOSED, UNKNOWN
    }

    private static final long serialVersionUID = 7228344342017879011L;
    private String id;
    private IssueStatus status;
    private String resolution;
    // Sometimes there will be a target release as well as fix versions.
    private String targetRelease;

    // Collections - i.e. flags, fixVersions etc.
    private List<Flag> flags;
    private Set<String> fixVersions;

    // Constructor.
    public JiraIssue(com.atlassian.jira.rest.client.domain.Issue issue) {

        this.id = issue.getKey();
        String statusString = issue.getStatus().getName().toUpperCase();
        statusString = statusString.replace(" ", "_");
        this.status = IssueStatus.valueOf(statusString);
        this.resolution = issue.getResolution() != null ? issue.getResolution().getName().toUpperCase() : "UNRESOLVED";

        // The target release part. Quite buggy at the minute.
        Field releaseField = issue.getFieldByName("Target Release");
        if (releaseField != null) {
            if (releaseField.getValue() != null) {
                this.targetRelease = cutTargetReleaseString(releaseField.getValue().toString());
            }
        } else {
            this.targetRelease = "UNSET";
        }

        // Build the flags. Set the list to size 6 since that is the typical no. of 'CDW' flags on a given JIRA issue.
        this.flags = new ArrayList<Flag>(6);

        // The field id's are constant from JIRA.
        flags.add(buildFlagFromField(issue.getFieldByName("CDW release")));
        flags.add(buildFlagFromField(issue.getFieldByName("CDW pm_ack")));
        flags.add(buildFlagFromField(issue.getFieldByName("CDW devel_ack")));
        flags.add(buildFlagFromField(issue.getFieldByName("CDW qa_ack")));
        flags.add(buildFlagFromField(issue.getFieldByName("CDW blocker")));
        flags.add(buildFlagFromField(issue.getFieldByName("CDW exception")));

        // Now something similar for the fix versions. We just have to get the
        this.fixVersions = findFixVersions(issue.getFixVersions());
        try {
            this.url = new URL(Constants.JIRA_BASE_BROWSE + id);
        } catch (MalformedURLException e) {
            Util.logException(LOG, "Invalid url: " + Constants.JIRA_BASE_BROWSE + id, e);
        }
    }

    @Override
    public String getNumber() {
        return id;
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public String getStatus() {
        return status.toString();
    }

    @Override
    public List<Flag> getFlags() {
        return flags;
    }

    @Override
    public Set<String> getFixVersions() {
        return fixVersions;
    }

    public String getResolution() {
        return this.resolution;
    }

    public String getTargetRelease() {
        return this.targetRelease;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\n[id=" + this.id + "]");
        builder.append("\n[status=" + this.status + "]");
        builder.append("\n[resolution=" + this.resolution + "]");
        builder.append("\n[targetRelease=" + this.targetRelease + "]");

        // The flags
        if (flags != null) {
            builder.append("\n[flags=");
            for (Flag f : flags) {
                // Only print out if the status is known.
                if (!f.getStatus().equals(Flag.Status.UNKNOWN)){
                    builder.append(" " + f.toString() + ",");
                }
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]");
        }

//        The fix versions
        builder.append("\n[fixVersions=");
        for (String s : fixVersions) {
            builder.append("(" + s + "),");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        builder.append("\n}");
        return builder.toString();
    }

    private Flag buildFlagFromField(Field field) {
        String name = field.getName();
        String statusValue = (String) field.getValue();
        Flag.Status status;
        if(statusValue == null) {
            status = Flag.Status.UNKNOWN;
        } else if (statusValue.equals("+")) {
            status = Flag.Status.POSITIVE;
        } else if (statusValue.equals("-")) {
            status = Flag.Status.NEGATIVE;
        } else if (statusValue.equals("?")) {
            status = Flag.Status.UNSET;
        } else {
            status = Flag.Status.UNKNOWN;
        }
        return new Flag(name, "{UNKNOWN_SETTER}", status);
    }


    private Set<String> findFixVersions(Iterable<Version> fixVersions) {
        SortedSet<String> toReturn = new TreeSet<String>();
        for (Version v : fixVersions) {
            toReturn.add(v.getName());
        }
        return toReturn;
    }

    private String cutTargetReleaseString(String releaseValue) {
        // Cut the description part out
        int descriptionStart = releaseValue.indexOf("description");
        int colonPosition = releaseValue.indexOf(":", descriptionStart);
        int commaSeparator = releaseValue.indexOf(",", colonPosition);
        String retVal = releaseValue.substring(colonPosition + 1, commaSeparator);
        return retVal.substring(1, retVal.length() - 1);
    }
}
