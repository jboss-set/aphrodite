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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/26/17.
 */
public class VersionComparatorTest {
    @Test
    public void testVersionComparation() {
        String upstreamRelease = "7.1.0.GA";
        String downstreamRelease = "7.0.7.GA";

        assertEquals("Comparing the same versions.", 0,
                VersionComparator.INSTANCE.compare(upstreamRelease, upstreamRelease));
        assertEquals("Comparing higher version with lower version.", 1,
                VersionComparator.INSTANCE.compare(upstreamRelease, downstreamRelease));
        assertEquals("Comparing lower version with higher version.", -1,
                VersionComparator.INSTANCE.compare(downstreamRelease, upstreamRelease));
    }
}