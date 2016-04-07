package org.jboss.set.aphrodite.config;

public enum StreamType {
    JSONSTREAM("jsonstream");

    private final String jsonstream;

    StreamType(final String jsonstream) {

        this.jsonstream = jsonstream;
    }

    @Override
    public String toString() {
        return jsonstream;
    }
}
