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
package org.jboss.set.aphrodite.container;

import javax.naming.NameNotFoundException;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class Container {
    private static final Container INSTANCE;

    static {
        final ServiceLoader<Container> loader = ServiceLoader.load(Container.class);
        INSTANCE = loader.iterator().next();
    }

    public static Container instance() {
        return INSTANCE;
    }

    /**
     * Retrieves the named object from the container. Note that it is unspecifed what the lifecycle of this object is.
     *
     * @param name the name of the object to look up
     * @param expected the expected class of the object
     * @return the object bound to <tt>name</tt>
     * @throws NameNotFoundException a minimal NameNotFoundException, do not expect many fields to be correctly filled
     */
    public abstract <T> T lookup(final String name, final Class<T> expected) throws NameNotFoundException;
}
