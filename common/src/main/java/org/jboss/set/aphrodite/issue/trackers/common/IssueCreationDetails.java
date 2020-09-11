/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.issue.trackers.common;

import java.net.URL;

/**
 * Abstract class for details required to create new issue. Each tracker has different requirements and package should provide
 * implementation that will fit those
 *
 * @author baranowb
 *
 */
public abstract class IssueCreationDetails {

    //description - top one;
    private String description;
    //ID of project, JBEAP, 'JBoss Enterprise Application Platform 6'
    private String projectKey;
    private URL trackerURL;

    public String getDescription() {
        return description;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public URL getTrackerURL() {
        return trackerURL;
    }

    public IssueCreationDetails setDescription(String description) {
        this.description = description;
        return this;
    }

    public IssueCreationDetails setProjectKey(String projectKey) {
        this.projectKey = projectKey;
        return this;
    }

    public IssueCreationDetails setTrackerURL(URL trackerURL) {
        this.trackerURL = trackerURL;
        return this;
    }

}
