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
package org.jboss.pull.shared.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * This test case is to show the actual danger of the generic. :-)
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class XMLRPCTestCase {
    @Test
    public void testStruct() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("Hello", 0xBEEFCA83);
        final Object struct = map;
        final Map<String, Object> result = XMLRPC.Struct.cast(struct);
        assertEquals(0xBEEFCA83, result.get("Hello"));
    }

    @Test
    public void testUnsafeStruct() {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(0xBEEFCA83, "Hello");
        final Object struct = map;
        final Map<String, Object> result = XMLRPC.Struct.cast(struct);
        assertTrue(result.containsKey(0xBEEFCA83)); // be very much aware!
    }
}
