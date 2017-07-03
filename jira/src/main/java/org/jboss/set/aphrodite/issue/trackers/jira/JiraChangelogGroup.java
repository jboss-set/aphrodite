/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.issue.trackers.jira;

import org.jboss.set.aphrodite.domain.User;

import java.util.Date;
import java.util.List;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/1/17.
 */
public class JiraChangelogGroup {
    private final User author;
    private final Date created;
    private final List<JiraChangelogItem> items;

    public JiraChangelogGroup(User author, Date created, List<JiraChangelogItem> items) {
        this.author = author;
        this.created = created;
        this.items = items;
    }

    public User getAuthor() {
        return author;
    }

    public Date getCreated() {
        return created;
    }

    public List<JiraChangelogItem> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        JiraChangelogGroup that = (JiraChangelogGroup) o;

        if (author != null ? !author.equals(that.author) : that.author != null)
            return false;
        if (created != null ? !created.equals(that.created) : that.created != null)
            return false;
        return items != null ? items.equals(that.items) : that.items == null;
    }

    @Override
    public int hashCode() {
        int result = author != null ? author.hashCode() : 0;
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (items != null ? items.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JiraChangelogGroup{" +
                "author=" + author +
                ", created=" + created +
                ", items=" + items +
                '}';
    }
}
