package org.jboss.set.aphrodite.domain;

public enum IssuePriority {

    BLOCKER(5), CRITICAL(4), MAJOR(3), MINOR(2), TRIVIAL(1), OPTIONAL(0), UNDEFINED(-1);

    final int priorityScore;

    private IssuePriority(final int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public boolean isHigherThan(IssuePriority otherPriority) {
        return (this.priorityScore > otherPriority.getPriorityScore());
    }

}
