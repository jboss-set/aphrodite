/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.stream.services.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.RepositoryType;
import org.jboss.set.aphrodite.domain.StreamComponent;

/**
 * Simple parser to compartmentalize all those nasty operations.
 *
 * @author baranowb
 *
 */
public class StreamComponentJsonParser {
    private static final Log LOG = LogFactory.getLog(StreamComponentJsonParser.class);
    public static final String JSON_NAME = "component_name";
    public static final String JSON_CONTACTS = "contacts";
    public static final String JSON_REPOSITORY_TYPE = "repository_type";
    public static final String JSON_REPOSITORY_URL = "repository_url";
    public static final String JSON_CODEBASE = "codebase";
    public static final String JSON_TAG = "tag";
    public static final String JSON_VERSION = "version";
    public static final String JSON_GAV = "gav";
    public static final String JSON_COMMENT = "comment";

    private StreamComponentJsonParser() {
    }

    public static StreamComponent parse(JsonObject json) {
        try {
            final String name = json.getString(JSON_NAME);
            final List<String> contacts = getContacts(json);
            final RepositoryType repositoryType = RepositoryType.fromType(json.getString(JSON_REPOSITORY_TYPE));
            String repository = json.getString(JSON_REPOSITORY_URL);
            URI repositoryURI = null;
            if (repository != null) {
                // TODO: retain fix from chao for now, check if we need it at all ?
                if (!repository.endsWith("/")) {
                    repository = repository + "/";
                }
                repositoryURI = new URI(repository);
            }
            final Codebase codeBase = new Codebase(json.getString(JSON_CODEBASE));
            final String tag = json.getString(JSON_TAG);
            final String version = json.getString(JSON_VERSION);
            final String gav = json.getString(JSON_GAV);
            final String comment = json.getString(JSON_COMMENT);
            return new StreamComponent(name, contacts, repositoryType, repositoryURI, codeBase, tag, version, gav, comment);
        } catch (Exception e) {
            Utils.logException(LOG, e);
            return null;
        }
    }

    private static List<String> getContacts(JsonObject json) {
        final JsonArray contactsArray = json.getJsonArray(JSON_CONTACTS);
        final List<String> contacts = new ArrayList<>(contactsArray.size());
        for (int index = 0; index < contactsArray.size(); index++) {
            contacts.add(contactsArray.getString(index));
        }
        return contacts;
    }

    public static JsonObject encodeStreamComponent(StreamComponent c) {
        final JsonObjectBuilder object = Json.createObjectBuilder();
        object.add(JSON_NAME, c.getName());
        object.add(JSON_CONTACTS, encodeContacts(c.getContacts()));
        object.add(JSON_REPOSITORY_TYPE, c.getRepositoryType().toString());
        object.add(JSON_REPOSITORY_URL, c.getRepositoryURL() == null ? "" : c.getRepositoryURL().toString());
        object.add(JSON_CODEBASE, c.getCodebase().getName());
        object.add(JSON_TAG, c.getTag());
        object.add(JSON_VERSION, c.getVersion());
        object.add(JSON_GAV, c.getGAV());
        object.add(JSON_COMMENT, c.getComment());
        return object.build();
    }

    private static JsonArrayBuilder encodeContacts(List<String> list) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (String s : list) {
            array.add(s);
        }
        return array;
    }
}
