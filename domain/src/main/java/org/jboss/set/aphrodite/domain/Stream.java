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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * this is the flag z-stream represented by a stream Usually the pattern is something like
 * jboss‑eap‑6.4.z
 *
 * @author egonzalez
 * @author baranowb
 */
public class Stream {

    private final String name;

    private final Stream upstream;

    private final Map<String, StreamComponent> components;

    //this is non encodable value, it is just here to identify JSON resource
    //that this stream belong to, so we know which one to serialize
    private final URL url;
    public Stream() {
        this("N/A");
    }

    public Stream(String name) {
        this(name, null);
    }

    public Stream(String name, Stream upstream) {
        this(null,name, upstream, new HashMap<>());
    }

    public Stream(URL url,String name, Stream upstream, Map<String, StreamComponent> components) {
        this.name = name;
        this.upstream = upstream;
        this.components = components;
        this.url = url;
        for(StreamComponent streamComponent:components.values()){
            streamComponent.setStream(this);
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasUpstream() {
        return upstream != null;
    }

    public Stream getUpstream() {
        return upstream;
    }

    public URL getURL() {
        return url;
    }

    public Collection<StreamComponent> getAllComponents() {
        return components.values();
    }

    public void addComponent(StreamComponent component) {
        components.put(component.getName(), component);
    }

    public void updateComponent(StreamComponent component) throws StreamComponentUpdateException {
        if(this.components.containsKey(component.getName())){
            components.put(component.getName(), component);
        } else {
            throw new StreamComponentUpdateException(component);
        }
    }

    public StreamComponent getComponent(String componentName) {
        return components.get(componentName.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Stream stream = (Stream) o;

        return name.equals(stream.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Stream{" +
                "name='" + name + '\'' +
                ", upstream=" + (upstream!=null?upstream.getName():"") +
                ",url="+url+
                ", components=" + components +
                '}';
    }
}
