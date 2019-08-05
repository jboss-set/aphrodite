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

import com.atlassian.jira.rest.client.api.domain.Version;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.NameNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/17/19.
 */
public class JiraReleaseTest {

    @Test
    public void testFindAll() throws NameNotFoundException {
        JiraIssueTracker is = Mockito.mock(JiraIssueTracker.class);
        List<Version> versions = new ArrayList<>();
        versions.add(new Version(null, null, "7.2.0.GA", null, false, true, null));
        versions.add(new Version(null, null, "7.2.0.CR1", null, false, true, null));
        versions.add(new Version(null, null, "7.2.0.CR2", null, false, true, null));
        versions.add(new Version(null, null, "7.2.0.ER1", null, false, true, null));
        versions.add(new Version(null, null, "7.2.0.DR1", null, false, true, null));
        versions.add(new Version(null, null, "7.3.0.GA", null, false, true, null));
        versions.add(new Version(null, null, "7.3.0.CR1", null, false, true, null));
        versions.add(new Version(null, null, "7.3.0.CR2", null, false, true, null));
        versions.add(new Version(null, null, "7.3.0.ER1", null, false, true, null));
        versions.add(new Version(null, null, "7.3.0.DR1", null, false, true, null));

        Mockito.when(is.getVersionsByProject("JBEAP")).thenReturn(versions);

        SimpleContainer container = (SimpleContainer) SimpleContainer.instance();
        container.register(JiraIssueTracker.class.getSimpleName(), is);


        Collection<JiraRelease> jiraReleases = JiraRelease.findAll();
        Assert.assertEquals(2, jiraReleases.size());
        jiraReleases.forEach(jiraRelease -> Assert.assertEquals(3, jiraRelease.getCandidateReleases().size()));
    }
}
