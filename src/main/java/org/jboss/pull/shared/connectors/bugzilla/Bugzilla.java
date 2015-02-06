package org.jboss.pull.shared.connectors.bugzilla;

public class Bugzilla extends CommentsClient {

    public Bugzilla(String serverUrl, String login, String password) {
        super(serverUrl, login, password);
    }

}
