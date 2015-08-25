package org.jboss.pull.shared.connectors.bugzilla;

import java.util.Map;

public class TrackerType {

    private final int id;
    private final boolean mustSend;
    private final boolean sendOnce;
    private final boolean canSend;
    private final String description;
    private final boolean canGet;
    private final String fullUrl;
    private final String url;

    public TrackerType(Map<String, Object> map) {
        this.id = (Integer) map.get("id");
        this.mustSend = (Boolean) map.get("must_send");
        this.sendOnce = (Boolean) map.get("send_once");
        this.canSend = (Boolean) map.get("can_send");
        this.description = (String) map.get("description");
        this.canGet = (Boolean) map.get("can_get");
        this.fullUrl = (String) map.get("fullUrl");
        this.url = (String) map.get("url");
    }

    public int getId() {
        return id;
    }

    public boolean isMustSend() {
        return mustSend;
    }

    public boolean isSendOnce() {
        return sendOnce;
    }

    public boolean isCanSend() {
        return canSend;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCanGet() {
        return canGet;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "TrackerType [id=" + id + ", mustSend=" + mustSend + ", sendOnce=" + sendOnce + ", canSend=" + canSend
                + ", description=" + description + ", canGet=" + canGet + ", fullUrl=" + fullUrl + ", url=" + url + "]";
    }
}