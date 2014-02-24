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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.pull.shared.connectors.common.Issue;


public class Bug implements Issue {

    // Bug Status
    public enum Status {
        NEW, ASSIGNED, POST, MODIFIED, ON_DEV, ON_QA, VERIFIED, RELEASE_PENDING, CLOSED
    }

    private static final long serialVersionUID = 6967220126171894474L;

    // includes attributes for Bug.get execution
    public static final Object[] include_fields = { "id", "alias", "product", "component", "version", "priority", "severity",
            "target_milestone", "creator", "assigned_to", "qa_contact", "docs_contact", "status", "resolution", "flags",
            "groups", "depends_on", "blocks", "target_release", "summary", "description" };

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

    public Bug(Map<String, Object> bugMap) {
        id = (Integer) bugMap.get("id");

        Object[] aliasObjs = (Object[]) bugMap.get("alias");
        alias = new ArrayList<String>(aliasObjs.length);
        for (Object obj : aliasObjs) {
            alias.add((String) obj);
        }

        product = (String) bugMap.get("product");

        Object[] componentObjs = (Object[]) bugMap.get("component");
        component = new ArrayList<String>(componentObjs.length);
        for (Object obj : componentObjs) {
            component.add((String) obj);
        }

        Object[] versionObjs = (Object[]) bugMap.get("version");
        version = new HashSet<String>(versionObjs.length);
        for (Object obj : versionObjs) {
            version.add((String) obj);
        }

        priority = (String) bugMap.get("priority");
        severity = (String) bugMap.get("severity");
        targetMilestone = (String) bugMap.get("target_milestone");
        creator = (String) bugMap.get("creator");
        assignedTo = (String) bugMap.get("assigned_to");
        qaContact = (String) bugMap.get("qa_contact");
        docsContact = (String) bugMap.get("docs_contact");
        status = Status.valueOf((String) bugMap.get("status"));
        resolution = (String) bugMap.get("resolution");

        flags = new ArrayList<Flag>();
        Object[] flagObjs = (Object[]) bugMap.get("flags");
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

        Object[] groupsObjs = (Object[]) bugMap.get("groups");
        groups = new ArrayList<String>(groupsObjs.length);
        for (Object obj : groupsObjs) {
            groups.add((String) obj);
        }

        Object[] dependsOnObjs = (Object[]) bugMap.get("depends_on");
        dependsOn = new HashSet<Integer>(dependsOnObjs.length);
        for (Object obj : dependsOnObjs) {
            dependsOn.add((Integer) obj);
        }

        Object[] blockObjs = (Object[]) bugMap.get("blocks");
        blocks = new HashSet<Integer>(blockObjs.length);
        for (Object obj : blockObjs) {
            blocks.add((Integer) obj);
        }

        Object[] targetReleaseObjs = (Object[]) bugMap.get("target_release");
        targetRelease = new HashSet<String>(targetReleaseObjs.length);
        for (Object obj : targetReleaseObjs) {
            targetRelease.add((String) obj);
        }

        summary = (String) bugMap.get("summary");
        description = (String) bugMap.get("description");
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

    @Override
    public String getUrl() {
        return "https://bugzilla.redhat.com/show_bug.cgi?id=" + id;
    }

    @Override
    public Set<String> getFixVersions() {
        return targetRelease;
    }

}
