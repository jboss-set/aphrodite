/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import org.jboss.set.aphrodite.spi.NotFoundException;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/17/19.
 */
public class CandidateJiraReleaseTest {

    @Test
    public void testIsGA() {
        Assert.assertTrue(CandidateRelease.isGA("7.4.22.GA"));
        Assert.assertFalse(CandidateRelease.isGA("7.4.22.GA.doc"));
        Assert.assertFalse(CandidateRelease.isGA("7.4.22.GA.RC1"));
    }

    @Test
    public void testIsGAorRC() {
        Assert.assertTrue(CandidateRelease.isCR("7.4.22.CR1"));
        Assert.assertTrue(CandidateRelease.isCR("7.4.22.CR10"));

        Assert.assertFalse(CandidateRelease.isCR("7.4.22.GA"));
        Assert.assertFalse(CandidateRelease.isCR("7.4.22.GA.doc"));
        Assert.assertFalse(CandidateRelease.isCR("7.4.22.GA.ER1"));
    }

    @Test
    public void extractVersion() throws NotFoundException {
        Assert.assertEquals("7.4.22", CandidateRelease.extractVersion("7.4.22.GA"));
        Assert.assertEquals("7.4.22", CandidateRelease.extractVersion("7.4.22.CR1"));
    }
}
