package org.jboss.set.aphrodite.service;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;

public interface IssueTrackerService {

    void init(Properties properties);

    boolean accepts(URL url);

    List<URL> findIssuesIn(Patch patch);

    Issue find(URL url) throws NotFoundException;

    List<Comment> findAll(Issue issue);

    void update(Issue issue);

}
