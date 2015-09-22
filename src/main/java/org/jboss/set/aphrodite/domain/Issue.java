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
 * Represents an issue in a issue tracker (bugzilla, jira...)
 *
 * @author egonzalez
 */
public class Issue {

    private URL url;

    // The unique id of an issue within its issue tracker domain e.g WFLY-5048
    private String trackerId;

    private String product; // E.g EAP6

    private String component; // E.g Clustering

    private String description;

    private String assignee;

    private Stage stage;

    private IssueStatus status;

    private IssueType type;

    private Release release;

    private List<Stream> streams;

    private List<URL> dependsOn;

    private List<URL> blocks;

    private IssueTracking tracking;

    private List<Comment> comments;

    public Issue(URL url) {
        this.url = url;
        this.trackerId = null;
        this.product = null;
        this.component = null;
        this.stage = new Stage();
        this.status = IssueStatus.UNDEFINED;
        this.type = IssueType.UNDEFINED;
        this.release = new Release();
        this.streams = new ArrayList<>();
        this.dependsOn = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.tracking = new IssueTracking();
        this.comments = new ArrayList<>();
    }

    public URL getURL() {
        return url;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
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

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
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

    public IssueTracking getTracking() {
        return tracking;
    }

    public void setTracking(IssueTracking tracking) {
        this.tracking = tracking;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "url=" + url +
                ", trackerId='" + trackerId + '\'' +
                ", product='" + product + '\'' +
                ", component='" + component + '\'' +
                ", description='" + description.substring(0, 10) + " ..." + '\'' +
                ", assignee='" + assignee + '\'' +
                ", stage=" + stage +
                ", status=" + status +
                ", type=" + type +
                ", release=" + release +
                ", streams=" + streams +
                ", dependsOn=" + dependsOn +
                ", blocks=" + blocks +
                ", tracking=" + tracking +
                ", #comments=" + comments.size() +
                '}';
    }
}
