package org.jboss.set.aphrodite.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StreamConfiguration {

    private Stream stream;

    private Map<Repository, Codebase> codebases;

    public StreamConfiguration(Stream stream) {
        this(stream, new HashMap<Repository, Codebase>());
    }

    public StreamConfiguration(Stream stream, Map<Repository, Codebase> codebases) {
        this.stream = stream;
        this.codebases = codebases;
    }

    public Stream getStream() {
        return stream;
    }

    public Set<Repository> getRespositories() {
        return codebases.keySet();
    }

    public Codebase getCodebaseFor(Repository repository) {
        return codebases.getOrDefault(repository, null);
    }

}
