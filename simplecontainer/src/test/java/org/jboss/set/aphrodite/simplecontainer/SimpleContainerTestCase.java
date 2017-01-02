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
package org.jboss.set.aphrodite.simplecontainer;

import org.jboss.set.aphrodite.container.Container;
import org.junit.Test;

import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleContainerTestCase {
    @Test
    public void testInstance() {
        final Container c1 = Container.instance();
        assertTrue(SimpleContainer.class.isInstance(c1));
        final Container c2 = Container.instance();
        assertSame(c1, c2);
    }

    @Test
    public void testWrongType() {
        final SimpleContainer container = (SimpleContainer) Container.instance();
        container.register("obj1", "Hello world");
        try {
            final Number n = container.lookup("obj1", Number.class);
            fail("Should have thrown NameNotFoundException");
        } catch (NameNotFoundException e) {
            final String msg = e.getMessage();
            // do not be too picky
            assertTrue(msg.contains("java.lang.Number"));
            assertTrue(msg.contains("java.lang.String"));
        }
    }
}
