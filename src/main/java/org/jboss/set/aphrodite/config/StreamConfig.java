/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.config;

import java.net.URL;
import java.util.Objects;

public class StreamConfig {
    private String location;
    private StreamType streamType;
    private URL url;

    public StreamConfig(URL url, StreamType streamType) {

        Objects.requireNonNull(url, "A 'url' must be specified for each service.");
        Objects.requireNonNull(streamType, "A 'streamType' must be specified for each service.");

        this.url = url;
        this.streamType = streamType;
    }

    public StreamConfig(String location, StreamType streamType) {

        Objects.requireNonNull(location, "A 'location' must be specified for each service.");
        Objects.requireNonNull(streamType, "A 'streamType' must be specified for each service.");

        this.location = location;
        this.streamType = streamType;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public String getLocation() {
        return location;
    }

    public URL getURL() {
        return url;
    }

    @Override
    public String toString() {
        return "StreamConfig {" +
                "streamType=" + getStreamType().toString() + "}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((streamType == null) ? 0 : streamType.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StreamConfig other = (StreamConfig) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (streamType != other.streamType)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }
}
