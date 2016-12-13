/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
