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

package org.jboss.set.aphrodite.config;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
public class AphroditeConfigTestUtils {

    public static void assertDeepEqualsIssueConfig(IssueTrackerConfig expected, IssueTrackerConfig result) {
        assertEquals("invalid url property read from valid JSON file", expected.getUrl(), result.getUrl());
        assertEquals("invalid user property read from valid JSON file", expected.getUsername(), result.getUsername());
        assertEquals("invalid password property read from valid JSON file", expected.getPassword(), result.getPassword());
        assertEquals("invalid tracker property read from valid JSON file", expected.getTracker(), result.getTracker());
    }

    public static void assertDeepEqualsRepositoryConfig(RepositoryConfig expected, RepositoryConfig result) {
        assertEquals("invalid url property read from valid JSON file", expected.getUrl(), result.getUrl());
        assertEquals("invalid user property read from valid JSON file", expected.getUsername(), result.getUsername());
        assertEquals("invalid password property read from valid JSON file", expected.getPassword(), result.getPassword());
        assertEquals("invalid type property read from valid JSON file", expected.getType(), result.getType());
    }
}
