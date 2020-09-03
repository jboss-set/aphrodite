package org.jboss.set.aphrodite.repository.services.common;

public enum RepositoryType {

    GITHUB("github"),
    GITLAB("gitlab");

    private final String type;

    RepositoryType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
