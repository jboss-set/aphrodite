package org.jboss.set.aphrodite.config;

public enum StreamType {
    JSON("json");

    private final String jsonstream;

    StreamType(final String jsonstream) {
        this.jsonstream = jsonstream;
    }

    @Override
    public String toString() {
        return jsonstream;
    }
}
