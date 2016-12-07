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

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class StreamConfig {
    private File streamFile;
    private StreamType streamType;
    private URL url;

    public StreamConfig(URL url, StreamType streamType) {
        Objects.requireNonNull(url, "A 'url' must be specified for each service.");
        Objects.requireNonNull(streamType, "A 'streamType' must be specified for each service.");
        this.url = url;
        this.streamType = streamType;
    }

    public StreamConfig(File streamFile, StreamType streamType) {
        Objects.requireNonNull(streamFile, "A 'streamFile' must be specified for each service.");
        Objects.requireNonNull(streamType, "A 'streamType' must be specified for each service.");
        this.streamFile = streamFile;
        this.streamType = streamType;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public Optional<File> getStreamFile() {
        return Optional.ofNullable(streamFile);
    }

    public Optional<URL> getURL() {
        return Optional.ofNullable(url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamConfig that = (StreamConfig) o;

        if (streamFile != null ? !streamFile.equals(that.streamFile) : that.streamFile != null) return false;
        if (streamType != that.streamType) return false;
        return url != null ? url.equals(that.url) : that.url == null;

    }

    @Override
    public int hashCode() {
        int result = streamFile != null ? streamFile.hashCode() : 0;
        result = 31 * result + (streamType != null ? streamType.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamConfig{" +
                "streamFile='" + streamFile + '\'' +
                ", streamType=" + streamType +
                ", url=" + url +
                '}';
    }
}
