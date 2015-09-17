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