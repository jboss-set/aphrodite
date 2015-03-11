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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pull.shared.connectors.common.Flag;
import org.jboss.pull.shared.connectors.common.Issue;

public class Bug implements Issue {

    // Bug Status
    public enum Status {
        NEW(0,"NEW"), ASSIGNED(1,"ASSIGNED"), POST(2,"POST"), MODIFIED(3,"MODIFIED"), ON_DEV(4,"ON_DEV"), ON_QA(5,"ON_QA"), VERIFIED(6,"VERIFIED"), RELEASE_PENDING(7,"RELEASE_PENDING"), CLOSED(8,"CLOSED");

        private final int step;
        private final String label;

        private Status(final int step, final String label) {
            this.step = step;
            this.label = label.toUpperCase();
        }

        public boolean hasPullRequest() {
            return ( step >= POST.step );
        }

        public boolean isAbove(Status status) {
            return ( step > status.step );
        }

        @Override
        public String toString() {
            return label;
        }
        public Status fromLabel(String label) {
            for ( Status status : Status.values() ) {
                if ( status.label.equalsIgnoreCase(label))
                    return status;
            }
            throw new IllegalArgumentException("No instance of " + Status.class + " associated with label:" + label);
        }
    }

    private static final long serialVersionUID = 6967220126171894474L;

    // includes attributes for Bug.get execution
    public static final Object[] include_fields = { "id", "alias", "product", "component", "version", "priority", "severity",
            "target_milestone", "creator", "assigned_to", "qa_contact", "docs_contact", "status", "resolution", "flags",
            "groups", "depends_on", "blocks", "target_release", "summary", "description", "cf_type", "creation_time" };

    private int id;
    private List<String> alias;
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
    private URL url;        // The issue URL.
    private String type;
    private Date creationTime;

    public Bug(Map<String, Object> bugMap) {
        id = (Integer) bugMap.get("id");

        alias = convertIntoStringList((Object[]) bugMap.get("alias"));

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

    @Override
    public String toString() {
        return "Bug [id=" + id + ", alias=" + alias + ", product=" + product + ", component=" + component + ", version="
                + version + ", priority=" + priority + ", severity=" + severity + ", targetMilestone=" + targetMilestone
                + ", creator=" + creator + ", assignedTo=" + assignedTo + ", qaContact=" + qaContact + ", docsContact="
                + docsContact + ", status=" + status + ", resolution=" + resolution + ", flags=" + flags + ", groups=" + groups
                + ", dependsOn=" + dependsOn + ", blocks=" + blocks + ", targetRelease=" + targetRelease + ", summary="
                + summary + ", description=" + description + ", url=" + url + ", type=" + type + ", creationTime="
                + creationTime + "]";
    }

}
