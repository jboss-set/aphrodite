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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    private List<String> components; // E.g Clustering

    private String summary;

    private String description;

    private String assignee;

    private String reporter;

    private Stage stage;

    private IssueStatus status;

    private IssueType type;

    private Release release;

    private Map<String, FlagStatus> streamStatus;

    private List<URL> dependsOn;

    private List<URL> blocks;

    private Date creationTime;

    private Date lastUpdated;

    private IssueEstimation estimation;

    private List<Comment> comments;

    public Issue(URL url) {
        if (url == null)
            throw new IllegalArgumentException("Issue URL cannot be null");

        this.url = url;
        this.stage = new Stage();
        this.status = IssueStatus.UNDEFINED;
        this.type = IssueType.UNDEFINED;
        this.release = new Release();
        this.streamStatus = new HashMap<>();
        this.dependsOn = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.components = new ArrayList<>();
    }

    public URL getURL() {
        return url;
    }

    public Optional<String> getTrackerId() {
        return Optional.ofNullable(trackerId);
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

    public Optional<String> getProduct() {
        return Optional.ofNullable(product);
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<String> getAssignee() {
        return Optional.ofNullable(assignee);
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Optional<String> getReporter() {
        return Optional.ofNullable(reporter);
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        Objects.requireNonNull(stage, "An Issue's stage cannot be set to null");
        this.stage = stage;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        Objects.requireNonNull(status, "An Issue's status cannot be set to null");
        this.status = status;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        Objects.requireNonNull(type, "An Issue's Type cannot be set to null");
        this.type = type;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        Objects.requireNonNull(release, "An Issue's Release cannot be set to null");
        this.release = release;
    }

    public Map<String, FlagStatus> getStreamStatus() {
        return streamStatus;
    }

    public void setStreamStatus(Map<String, FlagStatus> streamStatus) {
        Objects.requireNonNull(streamStatus, "An Issue's StreamStatus cannot be set to null");
        this.streamStatus = streamStatus;
    }

    public List<URL> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<URL> dependsOn) {
        Objects.requireNonNull(dependsOn, "An Issue's DependsOn List cannot be set to null");
        this.dependsOn = dependsOn;
    }

    public List<URL> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<URL> blocks) {
        Objects.requireNonNull(blocks, "An Issue's Blocks List cannot be set to null");
        this.blocks = blocks;
    }

    public Optional<Date> getCreationTime() {
        return Optional.ofNullable(creationTime);
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Optional<Date> getLastUpdated() {
        return Optional.ofNullable(lastUpdated);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Optional<IssueEstimation> getEstimation() {
        return Optional.ofNullable(estimation);
    }

    public void setEstimation(IssueEstimation estimation) {
        this.estimation = estimation;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        Objects.requireNonNull(comments, "An Issue's Comments cannot be set to null");
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "url=" + url +
                ", trackerId='" + trackerId + '\'' +
                ", product='" + product + '\'' +
                ", component='" + components + '\'' +
                ", summary='" + summary + '\'' +
                ", description='" + getPrintableDescription() + '\'' +
                ", assignee='" + assignee + '\'' +
                ", reporter='" + reporter + '\'' +
                ", stage=" + stage +
                ", status=" + status +
                ", type=" + type +
                ", release=" + release +
                ", streamStatus=" + streamStatus +
                ", dependsOn=" + dependsOn +
                ", blocks=" + blocks +
                ", creationDate=" + creationTime +
                ", lastUpdated=" + lastUpdated +
                ", estimation=" + estimation +
                ", #comments=" + comments.size() +
                "}\n";
    }

    private String getPrintableDescription() {
        if (description == null)
            return "";

        if (description.length() < 10)
            return description + "... ";
        return description.substring(0, 10) + "... ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Issue issue = (Issue) o;

        return url.equals(issue.url);

    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
