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

import javax.naming.NameNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleContainer extends Container {
    private final Map<String, Object> registry = new HashMap<>();

    @Override
    public <T> T lookup(final String name, final Class<T> expected) throws NameNotFoundException {
        final Object obj = registry.get(name);
        if (obj == null) throw new NameNotFoundException(name + " not found in registry");
        if (!expected.isInstance(obj)) throw new NameNotFoundException(name + " is of wrong type, expected " + expected + " got " + obj.getClass());
        return (T) obj;
    }

    public void register(final String name, final Object obj) {
        registry.put(name, obj);
    }
}
