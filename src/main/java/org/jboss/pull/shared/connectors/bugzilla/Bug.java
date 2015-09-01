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
package org.jboss.pull.shared.connectors.bugzilla;

import static org.jboss.pull.shared.connectors.bugzilla.ConversionUtils.convertIntoIntegerSet;
import static org.jboss.pull.shared.connectors.bugzilla.ConversionUtils.convertIntoStringList;
import static org.jboss.pull.shared.connectors.bugzilla.ConversionUtils.convertIntoStringSet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pull.shared.connectors.common.Flag;
import org.jboss.pull.shared.connectors.common.Issue;

public class Bug implements Issue {

    // Bug Status
    public enum Status {
        NEW, ASSIGNED, POST, MODIFIED, ON_DEV, ON_QA, VERIFIED, RELEASE_PENDING, CLOSED
    }

    private static final long serialVersionUID = 6967220126171894474L;

    // includes attributes for Bug.get execution
    public static final Object[] include_fields = { "id", "alias", "last_change_time", "product", "component", "version",
            "priority", "severity", "target_milestone", "creator", "assigned_to", "qa_contact", "docs_contact", "status",
            "resolution", "flags", "groups", "depends_on", "blocks", "target_release", "summary", "description", "cf_type",
            "creation_time", "estimated_time", "actual_time", "remaining_time", "external_bugs" };

    private int id;
    private List<String> alias;
    private Date last_change_time;
    private String product;
    private List<String> component;
    private Set<String> version;
    private String priority;
    private String severity;
    private String targetMilestone;
    private String creator;
    private String assignedTo;
    private String qaContact;
    private String docsContact;
    private Status status;
    private String resolution;
    private List<Flag> flags;
    private List<String> groups;
    private Set<Integer> dependsOn;
    private Set<Integer> blocks;
    private Set<String> targetRelease;
    private String summary;
    private String description;
    private URL url; // The issue URL.
    private String type;
    private Date creationTime;
    private Double estimated_time;
    private Double actual_time;
    private Double remaining_time;

    private Set<ExternalTrackerReference> externalTrackerRefs = new HashSet<ExternalTrackerReference>(0);

    @SuppressWarnings("unchecked")
    public Bug(Map<String, Object> bugMap) {
        id = (Integer) bugMap.get("id");

        alias = convertIntoStringList((Object[]) bugMap.get("alias"));
        last_change_time = (Date) bugMap.get("last_change_time");

        product = (String) bugMap.get("product");

        component = convertIntoStringList((Object[]) bugMap.get("component"));

        version = convertIntoStringSet((Object[]) bugMap.get("version"));

        priority = (String) bugMap.get("priority");
        severity = (String) bugMap.get("severity");
        targetMilestone = (String) bugMap.get("target_milestone");
        creator = (String) bugMap.get("creator");
        assignedTo = (String) bugMap.get("assigned_to");
        qaContact = (String) bugMap.get("qa_contact");
        docsContact = (String) bugMap.get("docs_contact");
        status = Status.valueOf((String) bugMap.get("status"));
        resolution = (String) bugMap.get("resolution");

        flags = constructFlagsFromObjectsArray((Object[]) bugMap.get("flags"));

        groups = convertIntoStringList((Object[]) bugMap.get("groups"));

        dependsOn = convertIntoIntegerSet((Object[]) bugMap.get("depends_on"));

        blocks = convertIntoIntegerSet((Object[]) bugMap.get("blocks"));

        targetRelease = convertIntoStringSet((Object[]) bugMap.get("target_release"));

        summary = (String) bugMap.get("summary");
        description = (String) bugMap.get("description");

        type = (String) bugMap.get("cf_type");

        creationTime = (Date) bugMap.get("creation_time");

        estimated_time = (Double) bugMap.get("estimated_time");
        actual_time = (Double) bugMap.get("actual_time");
        remaining_time = (Double) bugMap.get("remaining_time");

        Object[] external_bugs = (Object[]) bugMap.get("external_bugs");
        if (external_bugs != null && external_bugs.length > 0)
            for (Object o : external_bugs)
                externalTrackerRefs.add(new ExternalTrackerReference((Map<String, Object>) o));

        setUrl(id);
    }

    private void setUrl(int id) {
        try {
            this.url = new URL("https://bugzilla.redhat.com/show_bug.cgi?id=" + id);
        } catch (MalformedURLException malformed) {
            System.err.printf("Invalid URL formed: %s. \n", malformed);
        }
    }

    private static List<Flag> constructFlagsFromObjectsArray(Object[] flagObjs) {
        List<Flag> flags = new ArrayList<Flag>();
        for (Object obj : flagObjs) {
            @SuppressWarnings("unchecked")
            Map<String, Object> flag = (Map<String, Object>) obj;
            String name = (String) flag.get("name");
            String setter = (String) flag.get("setter");
            String s = (String) flag.get("status");
            Flag.Status status;

            if (s.equals(" ")) {
                status = Flag.Status.UNSET;
            } else if (s.equals("?")) {
                status = Flag.Status.UNKNOWN;
            } else if (s.equals("+")) {
                status = Flag.Status.POSITIVE;
            } else if (s.equals("-")) {
                status = Flag.Status.NEGATIVE;
            } else {
                throw new IllegalStateException("Unknown flag state");
            }

            flags.add(new Flag(name, setter, status));
        }
        return flags;
    }

    public int getId() {
        return id;
    }

    public List<String> getAlias() {
        return alias;
    }

    public Date getLastModified() {
        return last_change_time;
    }

    public void setLastModified(Date timestamp) {
        this.last_change_time = timestamp;
    }

    public String getProduct() {
        return product;
    }

    public List<String> getComponent() {
        return component;
    }

    public Set<String> getVersion() {
        return version;
    }

    public String getPriority() {
        return priority;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTargetMilestone() {
        return targetMilestone;
    }

    public String getCreator() {
        return creator;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getQaContact() {
        return qaContact;
    }

    public String getDocsContact() {
        return docsContact;
    }

    @Override
    public String getStatus() {
        return status.toString();
    }

    public String getResolution() {
        return resolution;
    }

    @Override
    public List<Flag> getFlags() {
        return flags;
    }

    public List<String> getGroups() {
        return groups;
    }

    public Set<Integer> getDependsOn() {
        return dependsOn;
    }

    public Set<Integer> getBlocks() {
        return blocks;
    }

    public Set<String> getTargetRelease() {
        return targetRelease;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getNumber() {
        return Integer.toString(id);
    }

    public String getType() {
        return type;
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public Set<String> getFixVersions() {
        return targetRelease;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public Double getEstimatedTime() {
        return estimated_time;
    }

    public void setEstimatedTime(Double estimated_time) {
        this.estimated_time = estimated_time;
    }

    public Double getActualTime() {
        return actual_time;
    }

    public void setActualTime(Double actual_time) {
        this.actual_time = actual_time;
    }

    public Double getRemainingTime() {
        return remaining_time;
    }

    public void setRemainingTime(Double remaining_time) {
        this.remaining_time = remaining_time;
    }

    public Set<ExternalTrackerReference> getExternalTrackerRefs() {
        return externalTrackerRefs;
    }

    public void setExternalTrackerRefs(Set<ExternalTrackerReference> externalTrackerRefs) {
        this.externalTrackerRefs = externalTrackerRefs;
    }

    @Override
    public String toString() {
        return "Bug [id=" + id + ", alias=" + alias + ", last_change_time=" + last_change_time + ", product=" + product
                + ", component=" + component + ", version=" + version + ", priority=" + priority + ", severity=" + severity
                + ", targetMilestone=" + targetMilestone + ", creator=" + creator + ", assignedTo=" + assignedTo
                + ", qaContact=" + qaContact + ", docsContact=" + docsContact + ", status=" + status + ", resolution="
                + resolution + ", flags=" + flags + ", groups=" + groups + ", dependsOn=" + dependsOn + ", blocks=" + blocks
                + ", targetRelease=" + targetRelease + ", summary=" + summary + ", description=" + description + ", url=" + url
                + ", type=" + type + ", creationTime=" + creationTime + ", estimated_time=" + estimated_time + ", actual_time="
                + actual_time + ", remaining_time=" + remaining_time + ", externalTrackerRefs=" + externalTrackerRefs + "]";
    }
}