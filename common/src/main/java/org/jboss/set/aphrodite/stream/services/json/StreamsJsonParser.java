/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.StreamService;

/**
 * Simple parser to compartmentalize all those nasty operations.
 *
 * @author baranowb
 *
 */
public class StreamsJsonParser {
    private static final Log LOG = LogFactory.getLog(StreamService.class);
    public static final String JSON_STREAMS = "streams";
    public static final String JSON_UPSTREAM = "upstream";
    public static final String JSON_CODEBASES = "codebases";
    public static final String JSON_NAME = "name";
    public static final String JSON_VALUE_NULL = "null";

    private StreamsJsonParser() {
    }

    public static Map<String, Stream> parse(final URL url) throws NotFoundException {
        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            JsonReader jr = Json.createReader(rd);

            final JsonArray jsonArray = jr.readObject().getJsonArray(JSON_STREAMS);
            Objects.requireNonNull(jsonArray, "streams array must be specified in json file");
            final Map<String, Stream> streamMap = new LinkedHashMap<String, Stream>();
            for (JsonValue value : jsonArray) {
                JsonObject json = (JsonObject) value;

                String upstreamName = json.getString(JSON_UPSTREAM, null);
                Stream upstream = streamMap.get(upstreamName);

                JsonArray codebases = json.getJsonArray(JSON_CODEBASES);
                Map<String, StreamComponent> codebaseMap = parseStreamCodebases(codebases);

                Stream currentStream = new Stream(url, json.getString(JSON_NAME), upstream, codebaseMap);
                streamMap.put(currentStream.getName(), currentStream);
            }
            return streamMap;
        } catch (Exception e) {
            Utils.logException(LOG, "Unable to load url: " + url.toString(), e);
            throw new NotFoundException(e);
        }
    }

    private static Map<String, StreamComponent> parseStreamCodebases(JsonArray codebases) {
        final Map<String, StreamComponent> codebaseMap = new LinkedHashMap<>();
        for (JsonValue value : codebases) {
            JsonObject json = (JsonObject) value;

            StreamComponent component = StreamComponentJsonParser.parse(json);
            if (component != null) {
                codebaseMap.put(component.getName(), component);
            }
        }
        return codebaseMap;
    }

    //this is just bread crumbing, but its easier to compartmentalize than deal with spectrum
    //of variables.
    public static JsonObject encode(Collection<Stream> toEncode) {
        //TODO, come up with some check or do we trust us?
        //assert toEncode instanceof LinkedHashMap;// no hanky pankies, we need ordered list, we had, otherwise it will screw us.
        final JsonObjectBuilder rootBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder array = encodeStreams(toEncode);
        rootBuilder.add(JSON_STREAMS, array);
        return rootBuilder.build();
    }

    private static JsonArrayBuilder encodeStreams(Collection<Stream> streams) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (Stream s : streams) {
            array.add(encodeStream(s));
        }
        return array;
    }

    private static JsonObjectBuilder encodeStream(Stream stream) {
        final JsonObjectBuilder object = Json.createObjectBuilder();
        object.add(JSON_NAME, stream.getName());
        object.add(JSON_UPSTREAM, stream.getUpstream() == null ? JSON_VALUE_NULL : stream.getUpstream().getName());
        object.add(JSON_CODEBASES, encodeStreamComponents(stream.getAllComponents()));
        return object;
    }

    private static JsonArrayBuilder encodeStreamComponents(Collection<StreamComponent> components) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (StreamComponent c : components) {
            array.add(StreamComponentJsonParser.encodeStreamComponent(c));
        }
        return array;
    }

}
