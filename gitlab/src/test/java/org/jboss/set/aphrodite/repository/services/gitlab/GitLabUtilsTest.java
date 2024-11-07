/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2024, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.repository.services.gitlab;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * <p>Tests for GitLabUtils.</p>
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class GitLabUtilsTest {

    @Test
    public void testGetProjectIdFromURL() throws Exception {
        URL url = new URL("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind");
        String projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/jackson-databind", projectId);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/jackson-databind");
        projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/xxx/jackson-databind", projectId);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind");
        projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind/-/merge_requests/2");
        projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/jackson-databind", projectId);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/merge_requests/2");
        projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/commits/master");
        projectId = GitLabUtils.getProjectIdFromURL(url);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);
    }

    @Test
    public void testGetProjectIdAndLastFieldFromURL() throws Exception {
        URL url = new URL("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind");
        String[] parts = GitLabUtils.getProjectIdAndLastFieldFromURL(url);
        Assert.assertNull(parts);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind/-/merge_requests/2");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURL(url);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/jackson-databind", parts[0]);
        Assert.assertEquals("2", parts[1]);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/jackson-databind/-/merge_requests/2");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURL(url);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/xxx/jackson-databind", parts[0]);
        Assert.assertEquals("2", parts[1]);

        url = new URL("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/issues/3");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURL(url);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", parts[0]);
        Assert.assertEquals("3", parts[1]);

    }

}
