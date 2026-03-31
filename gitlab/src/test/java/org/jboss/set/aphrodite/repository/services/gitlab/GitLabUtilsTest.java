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

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Tests for GitLabUtils.</p>
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class GitLabUtilsTest {

    @Test
    public void testGetProjectIdFromURL() throws Exception {
        URI uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind");
        String projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/jackson-databind", projectId);

        uri  = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/jackson-databind");
        projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/xxx/jackson-databind", projectId);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind");
        projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind/-/merge_requests/2");
        projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/jackson-databind", projectId);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/merge_requests/2");
        projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/commits/master");
        projectId = GitLabUtils.getProjectIdFromURI(uri);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", projectId);
    }

    @Test
    public void testGetProjectIdAndLastFieldFromURI() throws Exception {
        URI uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind");
        String[] parts = GitLabUtils.getProjectIdAndLastFieldFromURI(uri);
        Assert.assertNull(parts);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/jackson-databind/-/merge_requests/2");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURI(uri);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/jackson-databind", parts[0]);
        Assert.assertEquals("2", parts[1]);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/jackson-databind/-/merge_requests/2");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURI(uri);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/xxx/jackson-databind", parts[0]);
        Assert.assertEquals("2", parts[1]);

        uri = new URI("https://gitlab.xxx.redhat.com/jboss-set/xxx/yyy/jackson-databind/-/issues/3");
        parts = GitLabUtils.getProjectIdAndLastFieldFromURI(uri);
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.length);
        Assert.assertEquals("jboss-set/xxx/yyy/jackson-databind", parts[0]);
        Assert.assertEquals("3", parts[1]);

    }

}
