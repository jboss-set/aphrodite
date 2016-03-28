/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.repository.services;

import java.net.MalformedURLException;
import java.net.URL;
import static org.mockito.Mockito.when;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;

/**
 * Test get the commit status
 * 
 * @author Maoqian Chen (mchen@redhat.com)
 */
public class GitHubGetCommitStatusTest {

    @Mock
    Aphrodite aphrodite;
    private Patch singleComPatch;
    private Patch multipComPatch;
    private Patch otherStatPatch;
    private CommitStatus singleStatus;
    private CommitStatus multipStatus;
    private CommitStatus otherStatus;
    private URL singleURL;
    private URL multipURL;
    private URL otherURL;
    private String singleId = new String("49");
    private String multipId = new String("40");
    private String otherId = new String("15");

    @Before
    public void setup() throws MalformedURLException, NotFoundException {
        MockitoAnnotations.initMocks(this);
        singleURL = new URL("https://github.com/jboss-set/aphrodite/pull/49");
        multipURL = new URL("https://github.com/jboss-set/aphrodite/pull/40");
        otherURL = new URL("https://github.com/jboss-set/aphrodite/pull/15");

        singleComPatch = new Patch(singleId, singleURL, null, null, null);
        multipComPatch = new Patch(multipId, multipURL, null, null, null);
        otherStatPatch = new Patch(otherId, otherURL, null, null, null);

        when(aphrodite.getCommitStatusFromPatch(singleComPatch)).thenReturn(CommitStatus.SUCCESS);
        when(aphrodite.getCommitStatusFromPatch(multipComPatch)).thenReturn(CommitStatus.SUCCESS);
        when(aphrodite.getCommitStatusFromPatch(otherStatPatch)).thenReturn(CommitStatus.FAILURE);
    }

    @Test
    public void singleCommitTest() throws NotFoundException {
        singleStatus = aphrodite.getCommitStatusFromPatch(singleComPatch);
        assertEquals("bug single commit status misMatch", CommitStatus.SUCCESS, singleStatus);
    }

    @Test
    public void multipCommitTest() throws NotFoundException {
        multipStatus = aphrodite.getCommitStatusFromPatch(multipComPatch);
        assertEquals("bug multip commit status misMatch", CommitStatus.SUCCESS, multipStatus);
    }

    @Test
    public void otherStatusTest() throws NotFoundException {
        otherStatus = aphrodite.getCommitStatusFromPatch(otherStatPatch);
        assertEquals("bug other status miMatch", CommitStatus.FAILURE, otherStatus);
    }
}
