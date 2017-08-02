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
package org.jboss.set.aphrodite.domain;

public class PullRequestUpgrade {

    private final PullRequest pullRequest;
    private final String id;
    private final String tag;
    private final String version;
    private final String branch;

    public PullRequestUpgrade(PullRequest pullRequest, String id, String tag, String version, String branch) {
        super();
        this.pullRequest = pullRequest;
        this.id = id;
        this.tag = tag;
        this.version = version;
        this.branch = branch;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public String getVersion() {
        return version;
    }

    public String getBranch() {
        return branch;
    }

    public boolean hasEssentials() {
        return id != null && tag != null && version != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((branch == null) ? 0 : branch.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((pullRequest == null) ? 0 : pullRequest.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PullRequestUpgrade other = (PullRequestUpgrade) obj;
        if (branch == null) {
            if (other.branch != null)
                return false;
        } else if (!branch.equals(other.branch))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (pullRequest == null) {
            if (other.pullRequest != null)
                return false;
        } else if (!pullRequest.equals(other.pullRequest))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PullRequestUpgrade [pullRequest=" + pullRequest.getURL() + ", id=" + id + ", tag=" + tag + ", version="
                + version + ", branch=" + branch + "]";
    }

}
