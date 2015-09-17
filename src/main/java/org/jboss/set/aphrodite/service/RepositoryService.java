package org.jboss.set.aphrodite.service;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchStatus;
import org.jboss.set.aphrodite.domain.Repository;

public interface RepositoryService {

    void init(Properties properties);

    boolean accepts(URL url);

    Repository find(URL url) throws NotFoundException;

    List<URL> findPatchesIn(Issue issue);

    List<Patch> listByStatus(Repository repository, PatchStatus status);

    void addComment(Patch patch, String comment);
}
