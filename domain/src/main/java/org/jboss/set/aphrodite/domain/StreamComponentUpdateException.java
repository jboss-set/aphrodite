package org.jboss.set.aphrodite.domain;

public class StreamComponentUpdateException extends Exception {

    private static final long serialVersionUID = 7177859983014866843L;
    private final StreamComponent component;

    public StreamComponentUpdateException(final StreamComponent component) {
        super();
        this.component = component;
    }

    public StreamComponentUpdateException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace, final StreamComponent component) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.component = component;
    }

    public StreamComponentUpdateException(String message, Throwable cause, final StreamComponent component) {
        super(message, cause);
        this.component = component;
    }

    public StreamComponentUpdateException(String message, final StreamComponent component) {
        super(message);
        this.component = component;
    }

    public StreamComponentUpdateException(Throwable cause, final StreamComponent component) {
        super(cause);
        this.component = component;
    }

    public StreamComponent getStreamComponent() {
        return this.component;
    }
}
