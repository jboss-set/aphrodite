package org.jboss.set.aphrodite.domain;

public class Release {

    private String version;

    private String milestone;

    public Release() {
        this.version = "N/A";
        this.milestone = "--";
    }

    public Release (String version, String milestone) {
        this.version = version;
        this.milestone = milestone;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }
}
