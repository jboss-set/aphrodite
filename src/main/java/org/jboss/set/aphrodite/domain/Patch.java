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

import java.net.URL;

public class Patch {

    private final String id;
    private final URL url;
    private final Codebase codebase;
    private PatchState state;
    private String title;
    private String body;
    private Repository repository;

    public Patch(String id, URL url, Repository repository, Codebase codebase, PatchState state, String title, String body) {
        this.id = id;
        this.url = url;
        this.codebase = codebase;
        this.state = state;
        this.title = title;
        this.body = body;
        this.repository = repository;
    }

    public Patch(String id, URL url, Repository repository, Codebase codebase, PatchState state) {
        this(id, url, repository, codebase, state, null, null);

    }

    public String getId() {
        return id;
    }

    public URL getURL() {
        return url;

    }

    public Codebase getCodebase() {
        return codebase;
    }

    public PatchState getState() {
        return state;
    }

    public void setState(PatchState state) {
        this.state = state;
    }

    public String getTitle() {
        return title;

    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "Patch{" +
                "url=" + url +
                ", state=" + state +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", codebase=" + codebase +
                '}';
    }

}
