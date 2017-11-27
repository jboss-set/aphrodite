/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.cdicontainer;

import org.jboss.set.aphrodite.container.Container;
import org.jboss.weld.environment.se.Weld;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CDIContainerTestCase {
    private static Weld weld;

    @AfterClass
    public static void afterClass() {
        weld.shutdown();
    }

    @BeforeClass
    public static void beforeClass() {
        weld = new Weld();
        weld.initialize();
    }

    @Test
    public void testSimpleBean() throws NameNotFoundException {
        final SimpleBean bean = Container.instance().lookup("SimpleBean", SimpleBean.class);
        assertNotNull(bean);
    }

    @Test
    public void testSimpleBeanSameness() throws NameNotFoundException {
        final SimpleBean bean1 = Container.instance().lookup("SimpleBean", SimpleBean.class);
        assertNotNull(bean1);
        final SimpleBean bean2 = Container.instance().lookup("SimpleBean", SimpleBean.class);
        assertTrue(bean1 == bean2);
    }
}
