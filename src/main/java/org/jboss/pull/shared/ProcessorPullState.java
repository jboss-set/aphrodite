/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared;

/**
 * State of a pull request from the perspective of Pull Processor.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public enum ProcessorPullState {
    /**
     * The {@code NEW} represents a state of a pull request which has not been reviewed yet.
     */
    NEW,

    /**
     * The {@code PENDING} represents a state of a pull request which has already been triggered to merge.
     */
    PENDING,

    /**
     * The {@code RUNNING} represents a state of a pull request which is currently being merged.
     */
    RUNNING,

    /**
     * The {@code MERGEABLE} represents a state of a pull request which can be merged.
     */
    MERGEABLE,

    /**
     * The {@code INCOMPLETE} represents a state of a pull request which cannot be merged due to incompleteness of its description.
     */
    INCOMPLETE,

    /**
     * The {@code FINISHED} represents a state of a pull request which has been merged.
     */
    FINISHED,

    /**
     * The {@code MERGEABLE} represents a state of a pull request which can be checked due to an error.
     */
    ERROR
}
