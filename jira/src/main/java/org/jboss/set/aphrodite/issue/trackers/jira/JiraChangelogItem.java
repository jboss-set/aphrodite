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

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 6/1/17.
 */
public class JiraChangelogItem {
    private final String field;
    private final String from;
    private final String fromString;
    private final String to;
    private final String toString;

    public JiraChangelogItem(String field, String from, String fromString, String to, String toString) {
        this.field = field;
        this.from = from;
        this.fromString = fromString;
        this.to = to;
        this.toString = toString;
    }

    public String getField() {
        return this.field;
    }

    public String getFrom() {
        return this.from;
    }

    public String getFromString() {
        return this.fromString;
    }

    public String getTo() {
        return this.to;
    }

    public String getToString() {
        return this.toString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        JiraChangelogItem that = (JiraChangelogItem) o;

        if (field != null ? !field.equals(that.field) : that.field != null)
            return false;
        if (from != null ? !from.equals(that.from) : that.from != null)
            return false;
        if (fromString != null ? !fromString.equals(that.fromString) : that.fromString != null)
            return false;
        if (to != null ? !to.equals(that.to) : that.to != null)
            return false;
        return toString != null ? toString.equals(that.toString) : that.toString == null;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (fromString != null ? fromString.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (toString != null ? toString.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JiraChangelogItem{" +
                "field='" + field + '\'' +
                ", from='" + from + '\'' +
                ", fromString='" + fromString + '\'' +
                ", to='" + to + '\'' +
                ", toString='" + toString + '\'' +
                '}';
    }
}
