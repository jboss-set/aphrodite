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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * See http://ws.apache.org/xmlrpc/types.html
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class XMLRPC<T> {
    public static final XMLRPC<Object[]> Array = new XMLRPC<Object[]>(Object[].class);
    public static final XMLRPC<Map<String, Object>> Struct = new XMLRPC<Map<String, Object>>(Map.class);

    private final Class<T> cls;

    protected XMLRPC(final Class<?> cls) {
        this.cls = (Class<T>) cls;
    }

    public T cast(final Object obj) {
        // You might think the stuff below is useful, trust me, it is not.
        /*
        final ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        final Class<T> cls = (Class<T>) ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType();
        */
        return cls.cast(obj);
    }

    public static <T> T cast(final XMLRPC<T> type, Object obj) {
        return type.cast(obj);
    }

    // TODO: not entirely sure whether this one should be here
    public static <T> Iterable<T> iterable(final XMLRPC<T> type, final Collection<Object> c) {
        final Iterator<Object> it = c.iterator();
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public T next() {
                        return type.cast(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    public static <T> Iterable<T> iterable(final XMLRPC<T> type, final Object[] array) {
        final Iterator<Object> it = Arrays.asList(array).iterator();
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public T next() {
                        return type.cast(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }
}
