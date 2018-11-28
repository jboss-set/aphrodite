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
 * Main source of this code is:
 * https://github.com/wolfc/updepres/blob/master/model/src/main/java/org/jboss/up/depres/version/VersionComparator.java
 *
 * Compare 2 release versions
 * The version should have following format:
 * [<epoch>:]<upstream-version>-<product-version>
 */
import java.util.Comparator;

public class VersionComparator implements Comparator<String> {

    public static final VersionComparator INSTANCE = new VersionComparator();

    @Override
    public int compare(final String v1, final String v2) {
        int i1 = 0, i2 = 0;

        // Check epoch format [<epoch>:]
        // Get epoch version of the v1
        final int epoch1;
        int i = v1.indexOf(":");
        if (i != -1) {
            epoch1 = Integer.valueOf(v1.substring(0, i));
            i1 = i;
        }
        else
            epoch1 = 0;

        // Get epoch version of the v2
        final int epoch2;
        i = v2.indexOf(":");
        if (i != -1) {
            epoch2 = Integer.valueOf(v2.substring(0, i));
            i2 = i;
        }
        else
            epoch2 = 0;

        // Compare epochs versions
        if (epoch1 != epoch2)
            return epoch1 - epoch2;


        // Compare rest of the versions. They should have following format:
        // <upstream-version>-<product-version>

        final int lim1 = v1.length(), lim2 = v2.length();
        while (i1 < lim1 && i2 < lim2) {
            final char c1 = v1.charAt(i1);
            final char c2 = v2.charAt(i2);
            if (c1 == c2) {
                i1++;
                i2++;
            } else if (Character.isDigit(c1) || Character.isDigit(c2)) {
                int ei1 = i1, ei2 = i2;
                while (ei1 < lim1 && Character.isDigit(v1.charAt(ei1)))
                    ei1++;
                while (ei2 < lim2 && Character.isDigit(v2.charAt(ei2)))
                    ei2++;
                final int n1 = ei1 == i1 ? 0 : Integer.valueOf(v1.substring(i1, ei1));
                final int n2 = ei2 == i2 ? 0 : Integer.valueOf(v2.substring(i2, ei2));
                if (n1 != n2)
                    return n1 - n2;
                i1 = ei1;
                i2 = ei2;
            } else {
                // Return the difference between the characters
                return c1 - c2;
            }
        }
        // return the difference between the versions
        return lim1 - lim2;
    }

}