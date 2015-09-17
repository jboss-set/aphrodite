/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Map;

public class ExternalTrackerReference {

    private final  int id ;
    private final  int trackerId ;
    private final  String bugId ;
    private final  String priority ;
    private final  String description ;
    private final  TrackerType trackerType ;
    private final  String status;

    public ExternalTrackerReference(Map<String, Object> map) {
        this.id = (Integer) map.get("id");
        this.trackerId = (Integer) map.get("ext_bz_id");
        this.bugId = (String) map.get("ext_bz_bug_id");
        this.priority= (String) map.get("ext_priority");
        this.description = (String) map.get("ext_description");
        this.trackerType = new TrackerType((Map<String,Object>)map.get("type"));
        this.status = (String) map.get("ext_status");
    }

    public int getId() {
        return id;
    }

    public int getTrackerId() {
        return trackerId;
    }

    public String getBugId() {
        return bugId;
    }

    public String getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public TrackerType getTrackerType() {
        return trackerType;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ExternalTrackerReference [id=" + id + ", trackerId=" + trackerId + ", bugId=" + bugId + ", priority="
                + priority + ", description=" + description + ", trackerType=" + trackerType + ", status=" + status + "]";
    }
}