package org.jboss.set.aphrodite.domain;

import java.net.URL;

public class Patch {

    private URL url;

    private PatchStatus status;

    private String description;

    private Codebase codebase;

    public Patch(URL url, PatchStatus status, Codebase codebase) {
        this.url = url;
        this.status = status;
        this.codebase = codebase;
    }

    public URL getURL() {
        return url;
    }

    public PatchStatus getStatus() {
        return status;
    }

    public void setStatus(PatchStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Codebase getCodebase() {
        return codebase;
    }
}
