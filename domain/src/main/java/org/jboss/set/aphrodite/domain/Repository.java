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

import org.jboss.set.aphrodite.container.Container;
import org.jboss.set.aphrodite.domain.spi.CompareHome;

import javax.naming.NameNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Repository {

    private static final Pattern COMPONENT_VERSION = Pattern.compile("[+\\-]\\s+<version\\.(.*)>(.*)</version\\.(.*)>");

    private final URL url;

    private final List<Codebase> codebases = new ArrayList<>();

    public Repository(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return url;
    }

    public List<Codebase> getCodebases() {
        return codebases;
    }

    public Compare getCompare(String tag1, String tag2) throws NameNotFoundException {
        return Container.instance().lookup(CompareHome.class.getSimpleName(), (CompareHome.class)).getCompare(this.url, tag1, tag2);
    }

    public List<VersionUpgrade> getUpgradesForFile(String fileName, String tag1, String tag2) {
        try {
            String diff = getCompare(tag1, tag2).getDiffForFile(fileName);
            List<VersionUpgrade> upgrades = new ArrayList<>();
            String[] lines = diff.split(System.lineSeparator());
            String component = null, old = null;

            for (String line : lines) {
                Matcher m = COMPONENT_VERSION.matcher(line);
                if (m.find()) {
                    if (component == null) {
                        component = m.group(1);
                        old = m.group(2);
                    } else {
                        upgrades.add(new VersionUpgrade(component, old, m.group(2)));
                        component = null;
                    }
                }
            }

            return upgrades;
        } catch (NameNotFoundException nnfe) {
            return Collections.EMPTY_LIST;
        }
    }

    public List<VersionUpgrade> getComponentUpgrades(String tag1, String tag2) {
        return getUpgradesForFile("pom.xml", tag1, tag2);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Repository) {
            Repository that = (Repository) obj;
            return this.url.toString().equals(that.url.toString());
        }
        return false;
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
        return "Repository{" +
                "url=" + url +
                '}';
    }
}
