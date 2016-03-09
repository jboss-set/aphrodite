/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.issue.trackers.util;

import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
public class TestUtils {

    private static final double DELTA = 1e-15;

    public static void assertDeepEqualsIssue(Issue expected, Issue other) {
        assertEquals("issue tracker id mismatch", expected.getTrackerId(), other.getTrackerId());
        assertEquals("issue assignee mismatch", expected.getAssignee(), other.getAssignee());
        assertEquals("issue summary mismatch", expected.getSummary(), other.getSummary());
        assertEquals("issue creation time mismatch", expected.getCreationTime(), expected.getCreationTime());
        assertEquals("issue description mismatch", expected.getDescription(), other.getDescription());
        assertEquals("issue status mismatch", expected.getStatus(), other.getStatus());
        assertEquals("issue component mismatch", expected.getComponents(), other.getComponents());
        assertEquals("issue product mismatch", expected.getProduct(), other.getProduct());
        assertEquals("issue type mismatch", expected.getType(), other.getType());

        Release expectedRelease = expected.getRelease();
        Release otherRealease = other.getRelease();
        assertEquals("issue version mismatch", expectedRelease.getVersion(), otherRealease.getVersion());
        assertEquals("issue milestone mismatch", expectedRelease.getMilestone(), otherRealease.getMilestone());

        assertEquals("issue depends on list mismatch", expected.getDependsOn().size(), other.getDependsOn().size());
        assertEquals("issue depends on list mismatch", expected.getDependsOn(), other.getDependsOn());
        assertEquals("issue blocks list mismatch", expected.getBlocks().size(), other.getBlocks().size());
        assertEquals("issue blocks list mismatch", expected.getBlocks(), other.getBlocks());

        Optional<IssueEstimation> expectedEst = expected.getEstimation();
        Optional<IssueEstimation> otherEst = other.getEstimation();
        assertEquals("issue estimation mismatch", expectedEst.isPresent(), otherEst.isPresent());
        assertEquals("issue estimation mismatch", expectedEst.get().getInitialEstimate(), otherEst.get().getInitialEstimate(), DELTA);
        assertEquals("issue estimation mismatch", expectedEst.get().getHoursWorked(), otherEst.get().getHoursWorked(), DELTA);

        Stage expectedStage = expected.getStage();
        Stage otherStage = other.getStage();
        assertEquals("issue stage mismatch", expectedStage.getStateMap().size(), otherStage.getStateMap().size());
        assertEquals("issue stage mismatch", expectedStage.getStateMap(), otherStage.getStateMap());
    }
}
