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

package org.jboss.set.aphrodite.config;

import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;

import static org.jboss.set.aphrodite.config.AphroditeConfigTestUtils.assertDeepEqualsIssueConfig;
import static org.jboss.set.aphrodite.config.AphroditeConfigTestUtils.assertDeepEqualsRepositoryConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class AphroditeConfigTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AphroditeConfig template;

    private IssueTrackerConfig jiraConfig;
    private RepositoryConfig githubConfig;

    @Before
    public void setUp() {
        jiraConfig = new IssueTrackerConfig("https://issues.redhat.com/", "user", "pass", TrackerType.JIRA, 5);
        githubConfig = new RepositoryConfig("https://github.com/", "user", "pass", RepositoryType.GITHUB);

        when(template.getIssueTrackerConfigs()).thenReturn(Collections.singletonList(jiraConfig));
        when(template.getRepositoryConfigs()).thenReturn(Collections.singletonList(githubConfig));
        when(template.getStreamConfigs()).thenReturn(new ArrayList<>());
        when(template.getExecutorService()).thenReturn(Executors.newScheduledThreadPool(1));
    }

    @Test
    public void configCopyValidTest() {

        AphroditeConfig copy = new AphroditeConfig(template);
        assertNotNull("cannot create copy of valid configuration", copy);

        assertNotNull("error when copying issue config from valid configuration", copy.getIssueTrackerConfigs());
        assertEquals("error when copying issue config from valid configuration", copy.getIssueTrackerConfigs().size(), 1);
        assertNotNull("error when copying issue config from valid configuration", copy.getIssueTrackerConfigs().get(0));
        assertDeepEqualsIssueConfig(jiraConfig, copy.getIssueTrackerConfigs().get(0));

        assertNotNull("error when copying repository config from valid configuration", copy.getRepositoryConfigs());
        assertEquals("error when copying repository config from valid congiguration", copy.getIssueTrackerConfigs().size(), 1);
        assertNotNull("error when copying repository config from valid congiguration", copy.getRepositoryConfigs().get(0));
        assertDeepEqualsRepositoryConfig(githubConfig, copy.getRepositoryConfigs().get(0));
    }

    @Test
    public void configCopyNoIssueConfigTest() {
        expectedException.expect(NullPointerException.class);

        when(template.getIssueTrackerConfigs()).thenReturn(null);

        AphroditeConfig copy = new AphroditeConfig(template);
        assertNull(copy);
    }

    @Test
    public void configCopyNoRepositoryConfigTest() {
        expectedException.expect(NullPointerException.class);

        when(template.getRepositoryConfigs()).thenReturn(null);

        AphroditeConfig copy = new AphroditeConfig(template);
        assertNull(copy);
    }
}
