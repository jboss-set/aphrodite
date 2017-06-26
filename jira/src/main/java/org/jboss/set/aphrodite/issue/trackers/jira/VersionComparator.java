/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.issue.trackers.jira;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/15/17.
 */
import java.util.Optional;

public class VersionComparator{
    public static boolean isFirstVersionHigher(String first, String second) {
        if (first == null || second == null)
            return false;

        String[] firstParts = first.split("\\.");
        String[] secondParts = second.split("\\.");

        if (firstParts.length < 2 || secondParts.length < 2)
            return false;

        return areStringsNumericAndIsFirstBigger(firstParts[0], secondParts[0]) || areStringsNumericAndIsFirstBigger(firstParts[1], secondParts[1]);
    }

    private static boolean areStringsNumericAndIsFirstBigger(String firstStr, String secondStr) {
        int defaultValue = -1;
        int first = tryParseInteger(firstStr).orElse(defaultValue);
        int second = tryParseInteger(secondStr).orElse(defaultValue);
        return first != defaultValue && second != defaultValue && first > second;
    }

    private static Optional<Integer> tryParseInteger(String string) {
        try {
            return Optional.of(Integer.valueOf(string));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}