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