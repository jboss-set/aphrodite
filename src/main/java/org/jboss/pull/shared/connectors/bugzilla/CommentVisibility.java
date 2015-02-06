package org.jboss.pull.shared.connectors.bugzilla;

public enum CommentVisibility {

    PUBLIC(false), PRIVATE(true);

    private boolean visibility;

    CommentVisibility(final boolean visibility) {
        this.visibility = visibility;
    }

    public boolean isPrivate() {
        return visibility;
    }

}
