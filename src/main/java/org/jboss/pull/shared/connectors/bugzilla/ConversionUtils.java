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

package org.jboss.pull.shared.connectors.bugzilla;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConversionUtils {

    private ConversionUtils() {}

    public static Set<Integer> convertIntoIntegerSet(Object[] objectsArray) {
        Set<Integer> result = new HashSet<Integer>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((Integer) obj);
        }
        return result;
    }

    public static Set<String> convertIntoStringSet(Object[] objectsArray) {
        Set<String> result = new HashSet<String>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((String) obj);
        }
        return result;
    }

    public static List<String> convertIntoStringList(Object[] objectsArray) {
        List<String> result = new ArrayList<String>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((String) obj);
        }
        return result;
    }
}
