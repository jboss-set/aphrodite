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
import java.util.stream.Stream;

import javax.naming.NameNotFoundException;

import org.jboss.set.aphrodite.container.Container;
import org.jboss.set.aphrodite.domain.spi.IssueHome;
import org.jboss.set.aphrodite.domain.spi.PatchHome;

/**
 * Represents an issue in a issue tracker (bugzilla, jira...)
 *
 * @author egonzalez
 */
public class Issue {

    private URL url;

    // The unique id of an issue within its issue tracker domain e.g WFLY-5048
    private String trackerId;

    private final TrackerType trackerType;

    private String product; // E.g EAP6

    private List<String> components; // E.g Clustering

    private String summary;

    private String description;

    private User assignee;

    private User reporter;

    private Stage stage;

    private IssueStatus status;

    private String rawStatus;

    private IssuePriority priority;

    private IssueType type;

    private String rawType;

    private List<String> affectedVersions;

    private List<Release> releases;

    private Map<String, FlagStatus> streamStatus;

    private List<URL> dependsOn;

    private List<URL> blocks;

    private Date creationTime;

    private Date lastUpdated;

    private IssueEstimation estimation;

    private List<Comment> comments;

    /*
     * Modeling possible Patch URL in different PatchType:
     * 1. Pull Request: Git Pull Request e.g. https://github.com/jbossas/jboss-eap7/pull/1101
     * 2. Commit: Git or SVN commit e.g. https://github.com/hibernate/hibernate-orm/commit/b053116bb42330971ac1357009b2d8879e21b3f0
     * 3. File: Attachment Patch file URL
     */
    private List<Patch> patches;

    public Issue(URL url, TrackerType type) {
        if (url == null)
            throw new IllegalArgumentException("Issue URL cannot be null");

        this.url = url;
        this.trackerType = type;
        this.stage = new Stage();
        this.status = IssueStatus.UNDEFINED;
        this.rawStatus = IssueStatus.UNDEFINED.name();
        this.type = IssueType.UNDEFINED;
        this.rawType = IssueType.UNDEFINED.get();
        this.releases = new ArrayList<>();
        this.streamStatus = new HashMap<>();
        this.dependsOn = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.components = new ArrayList<>();
        this.patches = new ArrayList<>();
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

    public TrackerType getTrackerType() {
        return this.trackerType;
    }

    public List<String> getAffectedVersions() {
        return this.affectedVersions;
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

    public Optional<User> getAssignee() {
        return Optional.ofNullable(assignee);
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Optional<User> getReporter() {
        return Optional.ofNullable(reporter);
    }

    public void setReporter(User reporter) {
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

    @Deprecated
    public void setStatus(IssueStatus status) {
        setStatus(status, status.name());
    }

    public void setStatus(IssueStatus issueStatus, String rawStatus) {
        Objects.requireNonNull(issueStatus, "An Issue's status cannot be set to null");
        this.rawStatus = rawStatus.toUpperCase();
        this.status = issueStatus;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        Objects.requireNonNull(priority, "An Issue's priority cannot be set to null");
        this.priority = priority;
    }

    public IssueType getType() {
        return type;
    }

    @Deprecated
    public void setType(IssueType type) {
        setType(type, type.get());
    }

    public void setType(IssueType issueType, String rawType) {
        Objects.requireNonNull(issueType, "An Issue's Type cannot be set to null");
        this.rawType = rawType.toUpperCase();
        this.type = issueType;
    }

    public String getRawType() {
        return rawType;
    }

    public void setAffectedVersions(List<String> affectedVersions) {
        Objects.requireNonNull(affectedVersions);
        this.affectedVersions = affectedVersions;
    }


    public List<Release> getReleases() {
        return releases;
    }

    public void setReleases(List<Release> releases) {
        Objects.requireNonNull(releases, "An Issue's Release cannot be set to null");
        this.releases = releases;
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

    public Stream<Patch> getPatches() throws NameNotFoundException {
        return Container.instance().lookup(PatchHome.class.getSimpleName(), (PatchHome.class)).findPatchesByIssue(this);
    }

    public Stream<Issue> getUpstreamReferences() throws NameNotFoundException {
        return Container.instance().lookup(IssueHome.class.getSimpleName(), (IssueHome.class)).findUpstreamReferences(this);
    }

    @Override
    public String toString() {
        return "Issue{" +
                "url=" + url +
                ", trackerId='" + trackerId + '\'' +
                ", trackerType='" + trackerType + '\'' +
                ", product='" + product + '\'' +
                ", component='" + components + '\'' +
                ", summary='" + summary + '\'' +
                ", description='" + getPrintableDescription() + '\'' +
                ", assignee='" + assignee + '\'' +
                ", reporter='" + reporter + '\'' +
                ", stage=" + stage +
                ", status=" + status +
                ", rawStatus=" + rawStatus +
                ", priority=" + priority +
                ", type=" + type +
                ", rawType=" + rawType +
                ", release=" + releases +
                ", streamStatus=" + streamStatus +
                ", dependsOn=" + dependsOn +
                ", blocks=" + blocks +
                ", creationDate=" + creationTime +
                ", lastUpdated=" + lastUpdated +
                ", estimation=" + estimation +
                ", #comments=" + comments.size() +
                ", #patches=" + patches.size() +
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Issue other = (Issue) obj;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (trackerId == null) {
            if (other.trackerId != null)
                return false;
        } else if (!trackerId.equals(other.trackerId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((trackerId == null) ? 0 : trackerId.hashCode());
        return result;
    }
}
