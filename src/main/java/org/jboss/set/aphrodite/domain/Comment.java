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

package org.jboss.set.aphrodite.domain;

import java.util.Objects;
import java.util.Optional;

public class Comment {

    private final String parentIssueId;

    private final String id;

    private final String body;

    private final boolean isPrivate;

    public Comment(String parentIssueId, String id, String body, boolean isPrivate) {
        Objects.requireNonNull(body, "A comment cannot have a null body.");
        this.parentIssueId = parentIssueId;
        this.id = id;
        this.body = body;
        this.isPrivate = isPrivate;
    }

    public Comment(String id, String body, boolean isPrivate) {
        this(null, id, body, isPrivate);
    }

    public Comment(String body, boolean isPrivate) {
        this(null, null, body, isPrivate);
    }

    public Optional<String> getParentIssueId() {
        return Optional.ofNullable(parentIssueId);
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public String getBody() {
        return body;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "parentIssueId='" + parentIssueId + '\'' +
                ", id='" + id + '\'' +
                ", body='" + body + '\'' +
                ", isPrivate=" + isPrivate +
                '}';
    }
}
