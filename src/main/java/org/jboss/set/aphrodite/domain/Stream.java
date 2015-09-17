package org.jboss.set.aphrodite.domain;


/**
 * this is the flag z-stream represented by a stream
 * Usually the pattern is something like jboss‑eap‑6.4.z
 * @author egonzalez
 *
 */
public class Stream {

    private String name;

    private FlagStatus status;

    public Stream() {
        this.name = "N/A";
        this.status = FlagStatus.NO_SET;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public FlagStatus getStatus() {
        return status;
    }

    public void setStatus(FlagStatus status) {
        this.status = status;
    }
}
