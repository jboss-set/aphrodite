/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared.connectors.github;

import static org.testng.Assert.assertNotNull;

import java.net.URL;

import org.eclipse.egit.github.core.Label;
import org.jboss.pull.shared.PullHelper;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class GithubHelperTestCase {
    @Test
    public void testLabelWithSpace() throws Exception {
        final URL url = getClass().getResource("/processor-uselessorg.properties");
        final PullHelper helper = new PullHelper("dummy", url.getPath());
        final Label label = helper.getLabel("with a space");
        assertNotNull(label);
    }
}
