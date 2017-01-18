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

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * @author Ryan Emerson
 * @author baranowb
 */
public class StreamComponent {
    private final String name;
    private final List<String> contacts;
    private final RepositoryType repositoryType;
    private final URI repositoryURL;
    // branch used to build
    private final Codebase codebase;
    // latest tag being used as build point
    private final String tag;
    // version included in build
    private final String version;
    // maven GAV, in reality it is group id
    private final String gav;
    // Just a comment for us petty humans.
    private final String comment;

    public StreamComponent(final String name, final List<String> contacts, final RepositoryType repositoryType,
            final URI repositoryURL, final Codebase codebase, final String tag, final String version, final String gav,
            final String comment) {
        super();
        this.name = name;
        this.contacts = contacts;
        this.repositoryType = repositoryType;
        this.repositoryURL = repositoryURL;
        this.codebase = codebase;
        this.tag = tag;
        this.version = version;
        this.gav = gav;
        this.comment = comment;
    }

    public StreamComponent(final String name, final List<String> contacts, final URI repositoryURL, final Codebase codebase,
            final String tag, final String version, final String gav, final String comment) {
        this(name, contacts, RepositoryType.fromRepositoryURI(repositoryURL), repositoryURL, codebase, tag, version, gav,
                comment);
    }

    public StreamComponent(final String name, final List<String> contacts, final URI repositoryURL, final Codebase codebase,
            final String tag, final String version, final String gav) {
        this(name, contacts, repositoryURL, codebase, tag, version, gav, null);
    }

    public String getName() {
        return name;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public RepositoryType getRepositoryType() {
        return repositoryType;
    }

    public URI getRepositoryURL() {
        return repositoryURL;
    }

    public Codebase getCodebase() {
        return codebase;
    }

    public String getTag() {
        return tag;
    }

    public String getVersion() {
        return version;
    }

    public String getGAV() {
        return gav;
    }

    public String getComment() {
        return comment;
    }

    // Returns a String only if a rule has been established for the host.
    // This is necessary to cater for any components hosted outside of github.
    public Optional<String> getCodeBasePath() {
        String url = repositoryURL.toString();
        if (url.contains("github.com")) {
            if (!url.endsWith("/"))
                url += "/";
            return Optional.of(url + "tree/" + codebase.getName());
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        StreamComponent streamComponent = (StreamComponent) o;
        return name.equals(streamComponent.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "StreamComponent [name=" + name + ", contacts=" + contacts + ", repositoryType=" + repositoryType
                + ", repositoryURL=" + repositoryURL + ", codebase=" + codebase + ", tag=" + tag + ", version=" + version
                + ", gav=" + gav + ", comment=" + comment + "]";
    }
}
