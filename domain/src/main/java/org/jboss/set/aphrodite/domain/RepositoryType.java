/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.domain;

import java.net.URI;

/**
 * Represents a type of repository.
 *
 * @author baranowb
 */
public enum RepositoryType {
    SVN, GIT, CVS, ARCHIVE, UNKNOWN;
    public static RepositoryType fromRepositoryURL(final String stringURI) {
        if (stringURI.startsWith("git:") || stringURI.endsWith(".git") || stringURI.contains("github")
                || stringURI.contains("git.app")) {
            return RepositoryType.GIT;
        } else if (stringURI.startsWith("svn:") || stringURI.contains("svn") || stringURI.contains("trunk")) {
            return RepositoryType.SVN;
        } else if (stringURI.startsWith(":pserver:") || stringURI.contains("cvsroot")) {
            return RepositoryType.CVS;
        } else if (stringURI.endsWith(".jar") || stringURI.endsWith(".gz") || stringURI.endsWith(".zip")) {
            return RepositoryType.ARCHIVE;
        } else {
            return RepositoryType.UNKNOWN;
        }
    }

    public static RepositoryType fromRepositoryURI(final URI uri) {
        return fromRepositoryURL(uri.toString());
    }

    public static RepositoryType fromType(final String type) {
        if (type.toLowerCase().equals("git")) {
            return GIT;
        } else if (type.toLowerCase().equals("svn")) {
            return SVN;
        } else if (type.toLowerCase().equals("cvs")) {
            return CVS;
        } else if (type.toLowerCase().equals("archive")) {
            return ARCHIVE;
        } else {
            return UNKNOWN;
        }
    }
}
