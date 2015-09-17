package org.jboss.set.aphrodite.domain;

public class Comment {

    private String id;

    private String body;

    public Comment(String id, String body) {
        this.id = id;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public String getId() {
        return id;
    }
}
