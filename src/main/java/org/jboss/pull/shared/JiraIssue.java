/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JIRA issue representation.
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class JiraIssue implements Issue {

    private static final long serialVersionUID = 7228344342017879011L;

    private String id;
    private String status;
    private List<Flag> flags;
    private Set<String> fixVersions;

    public JiraIssue(final String id, final String status, final List<Flag> flags, final Set<String> fixVersions) {
        this.id = id;
        this.status = status;
        this.flags = new ArrayList<Flag>(flags);
        this.fixVersions = fixVersions;
    }

    @Override
    public String getNumber() {
        return id;
    }

    @Override
    public String getUrl() {
        return "https://issues.jboss.org/browse/" + id;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public List<Flag> getFlags() {
        return flags;
    }

    @Override
    public Set<String> getFixVersions() {
        return fixVersions;
    }
}
