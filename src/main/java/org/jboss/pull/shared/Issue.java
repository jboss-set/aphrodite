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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Represents a general issue.
 * Typically a JIRA issue or a Bugzilla bug.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public interface Issue extends Serializable {
    /** number/id of the issue */
    String getNumber();
    /** url of the issue in the issue tracker */
    String getUrl();
    /** status of the issue */
    String getStatus();
    /** flags of the issue */
    List<Flag> getFlags();   // our Flag class will be enough for both Bugzilla and Jira hopefully

    /** Jira Fix Version/s, Bugzilla calls Target Release **/
    Set<String> getFixVersions();
}
