package org.jboss.set.aphrodite.spi;

import java.util.List;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Violation;

public interface JanitorService {

    /**
     * Initialize the {@link JanitorService} instance
     * @param config
     * @return
     */
    boolean init(Aphrodite aphrodite);

    /**
     * Returns all {@link Violation}s associated to the {@link Issue} provided.
     *
     * @param issue instance of {@link Issue} to analyze
     * @return
     */
    List<Violation> getIssueViolations(Issue issue);
}
