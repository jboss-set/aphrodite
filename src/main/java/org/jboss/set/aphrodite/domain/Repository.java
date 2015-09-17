package org.jboss.set.aphrodite.domain;

import java.net.URL;
import java.util.List;

public class Repository {

    private URL url;

    private List<Codebase> codebases;

    public Repository(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return url;
    }

    public List<Codebase> getCodebases() {
        return codebases;
    }
}
