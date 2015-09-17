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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * representes a isuse in a issue tracker (bugzilla, jira...)
 * @author egonzalez
 *
 */
public class Issue {

    private URL url;

    private String description;

    private String assignee;

    private Stage stage;

    private IssueStatus status;

    private IssueType type;

    private Release release;

    private List<Stream> streams;

    private List<URL> dependsOn;

    private List<URL> blocks;

    public Issue(URL url) {
        this.url = url;
        this.stage = new Stage();
        this.status = IssueStatus.UNDEFINED;
        this.type = IssueType.UNDEFINED;
        this.release = new Release();
        this.streams = new ArrayList<Stream>();
        this.dependsOn = new ArrayList<URL>();
        this.blocks = new ArrayList<URL>();
    }

    public URL getURL() {
        return url;
    }

    public Stage getStage() {
        return stage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public Release getRelease() {
        return release;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public List<URL> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<URL> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<URL> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<URL> blocks) {
        this.blocks = blocks;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
