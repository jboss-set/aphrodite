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

package org.jboss.set.aphrodite.domain;

import java.util.Optional;

public class Release {

    private String version;
    private String milestone;

    public Release() {
    }

    public Release(String version, String milestone) {
        this.version = version;
        this.milestone = milestone;
    }

    public Release(String version) {
        this(version, null);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    public Optional<String> getMilestone() {
        return Optional.ofNullable(milestone);
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Release release = (Release) o;

        if (version != null ? !version.equals(release.version) : release.version != null) return false;
        return milestone != null ? milestone.equals(release.milestone) : release.milestone == null;
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (milestone != null ? milestone.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Release{" +
                "version='" + version + '\'' +
                ", milestone='" + milestone + '\'' +
                '}';
    }
}
