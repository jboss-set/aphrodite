package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import org.jboss.set.aphrodite.domain.IssuePriority;

/**
 * <p>
 * Bugzilla priority naming differs from the one offered by this API, and has been mapped like that:
 * </p>
 *
 * <ol>
 * <li>BLOCKER - no bugzilla equivalent ,</li>
 * <li>CRITICAL - maps to URGENT,</li>
 * <li>MAJOR(3), // Bugzilla, maps to HIGH,</li>
 * <li>MINOR(2), // Bugzilla, maps to MEDIUM,</li>
 * <li>TRIVIAL(1), // Bugzilla, maps to LOW,</li>
 * <li>OPTIONAL(0), // Bugzilla, no equivalent.</li>
 * </ol>
 *
 * <p>
 * This utility class is designed to handle all logic related to such translation (and nothing else).
 * </p>
 *
 * @author Romain Pelisse <romain@redhat.com>
 *
 */
public final class IssuePriorityTranslatorUtil {

    private IssuePriorityTranslatorUtil() {
    };

    public static IssuePriority translateFromBugzilla(String priority) {
        if (priority == null)
            return IssuePriority.UNDEFINED;
        else {
            switch (priority.toLowerCase()) {
                case "urgent":
                    return IssuePriority.CRITICAL;
                case "high":
                    return IssuePriority.MAJOR;
                case "medium":
                    return IssuePriority.MINOR;
                case "low":
                    return IssuePriority.TRIVIAL;
                default:
                    return IssuePriority.UNDEFINED;
            }
        }
    }
}
