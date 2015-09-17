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
