package org.jboss.set.aphrodite.repository.services.common;

public enum RepositoryType {

    GITHUB("github");

    private String type;

    private RepositoryType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
