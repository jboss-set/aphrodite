/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.net.URL;

/*
 * Modeling possible Patch in different PatchType:
 * 1. Pull Request URL: Git Pull Request e.g. https://github.com/jbossas/jboss-eap7/pull/1101
 *    In Jira, it looks for URL in field 'Git Pull Request'.
 *    In Bugzilla, it looks for legitimate Git URL in comments.
 * 2. Commit URL: Git or SVN commit e.g. https://github.com/hibernate/hibernate-orm/commit/b053116bb42330971ac1357009b2d8879e21b3f0
 *    In Jira, it looks for URL in field 'Git Pull Request'.
 *    In Bugzilla, it looks for legitimate Git/SVN commit URL in comments.
 * 3. File URL: Attachment Patch file URL.
 */

public class Patch {

    private final URL url;
    private final PatchType patchType;
    private final PatchState patchState;

    public Patch(URL url, PatchType patchType, PatchState patchState) {
        this.url = url;
        this.patchType = patchType;
        this.patchState = patchState;
    }

    public URL getUrl() {
        return url;
    }

    public PatchType getPatchType() {
        return patchType;
    }

    public PatchState getPatchState() {
        return patchState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Patch patch = (Patch) o;

        return url.equals(patch.url);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Patch{" + "type=" + patchType + "url=" + url + "patchState=" + patchState + '}';
    }
}
