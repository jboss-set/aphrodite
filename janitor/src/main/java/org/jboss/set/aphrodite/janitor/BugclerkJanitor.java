package org.jboss.set.aphrodite.janitor;

import java.util.Collections;
import java.util.List;

import org.jboss.jbossset.bugclerk.BugClerk;
import org.jboss.jbossset.bugclerk.BugclerkConfiguration;
import org.jboss.jbossset.bugclerk.aphrodite.AphroditeClient;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Violation;
import org.jboss.set.aphrodite.spi.JanitorService;

public class BugclerkJanitor implements JanitorService {

    private BugClerk bugclerk;

    @Override
    public boolean init(Aphrodite aphrodite) {
        AphroditeClient client = new AphroditeClient(aphrodite);
        bugclerk = new BugClerk(client, new BugclerkConfiguration());
        return true;
    }

    @Override
    public List<Violation> getIssueViolations(
            Issue issue) {
        return bugclerk.getViolationsOnIssue(issue, Collections.emptyList());
    }

    public BugClerk getBugclerk() {
        return bugclerk;
    }

    public void setBugclerk(BugClerk bugclerk) {
        this.bugclerk = bugclerk;
    }

}
