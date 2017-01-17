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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.StreamConfig;
import org.jboss.set.aphrodite.config.StreamType;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.StreamService;

/**
 * A stream service which reads stream data from the specified JSON file.  This implementation
 * assumes that streams are written in order in the json file, i.e. the most recent (upstream) issue
 * is specified as the first JSON object in the "streams" JSON array. An example JSON file can be
 * found at https://github.com/jboss-set/jboss-streams
 *
 * @author Ryan Emerson
 */
public class JsonStreamService implements StreamService {
    private static final Log LOG = LogFactory.getLog(JsonStreamService.class);

    private final Map<String, Stream> streamMap = new HashMap<>();
    private Aphrodite aphrodite;
    private AphroditeConfig config;
    @Override
    public boolean init(Aphrodite aphrodite, AphroditeConfig config) throws NotFoundException {
        this.aphrodite = aphrodite;
        this.config = config;
        return updateStreams();
    }

    private boolean init(StreamConfig config) throws NotFoundException {
        if (config.getURL().isPresent()) {
            readJsonFromURL(config.getURL().get());
        } else if (config.getStreamFile().isPresent()) {
            readJsonFromFile(config.getStreamFile().get());
        } else {
            throw new IllegalArgumentException("StreamConfig requires either a URL or File to be specified");
        }
        return true;
    }

    @Override
    public synchronized boolean updateStreams() throws NotFoundException {
        Iterator<StreamConfig> i = this.config.getStreamConfigs().iterator();
        while (i.hasNext()) {
            StreamConfig streamConfig = i.next();
            if (streamConfig.getStreamType() == StreamType.JSON) {
                i.remove();
                return init(streamConfig);
            }
        }
        return false;
    }

    @Override
    public synchronized List<Stream> getStreams() {
        return new ArrayList<>(streamMap.values());
    }

    @Override
    public synchronized Stream getStream(String streamName) {
        return streamMap.get(streamName);
    }

    private void readJsonFromFile(File file) throws NotFoundException {
        try (JsonReader jr = Json.createReader(new FileInputStream(file))) {
            parseJson(jr.readObject());
        } catch (IOException e) {
            Utils.logException(LOG, "Unable to load file: " + file.getPath(), e);
            throw new NotFoundException("Unable to load file: " + file.getPath(), e);
        } catch (JsonException e) {
            Utils.logException(LOG, e);
            throw new NotFoundException(e);
        }
    }

    private void readJsonFromURL(URL url) throws NotFoundException {
        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            JsonReader jr = Json.createReader(rd);
            parseJson(jr.readObject());
        } catch (IOException | NotFoundException e) {
            Utils.logException(LOG, "Unable to load url: " + url.toString(), e);
            throw new NotFoundException(e);
        }
    }

    private void parseJson(JsonObject jsonObject) throws NotFoundException {
        JsonArray jsonArray = jsonObject.getJsonArray("streams");
        Objects.requireNonNull(jsonArray, "streams array must be specified in json file");

        for (JsonValue value : jsonArray) {
            JsonObject json = (JsonObject) value;

            String upstreamName = json.getString("upstream", null);
            Stream upstream = streamMap.get(upstreamName);

            JsonArray codebases = json.getJsonArray("codebases");
            Map<String, StreamComponent> codebaseMap = parseStreamCodebases(codebases);

            Stream currentStream = new Stream(json.getString("name"), upstream, codebaseMap);
            streamMap.put(currentStream.getName(), currentStream);
        }
    }

    private Map<String, StreamComponent> parseStreamCodebases(JsonArray codebases) {
        Map<String, StreamComponent> codebaseMap = new HashMap<>();
        for (JsonValue value : codebases) {
            JsonObject json = (JsonObject) value;

            StreamComponent component = StreamComponentJsonParser.parse(json);
            if (component != null) {
                codebaseMap.put(component.getName(), component);
            }
        }
        return codebaseMap;
    }

    @Override
    public List<URI> getDistinctURLRepositories() {
        return getStreams().stream()
                .flatMap(stream -> getDistinctURLRepositoriesByStream(stream.getName()).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<URI> getDistinctURLRepositoriesByStream(String streamName) {
        Stream stream = getStream(streamName);
        if (stream == null)
            return new ArrayList<>();

        return stream.getAllComponents().stream()
                .map(StreamComponent::getRepositoryURL)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Stream> getStreamsBy(URI repositoryURL, Codebase codebase) {
        List<Stream> streams = new ArrayList<>();
        for (Stream stream : getStreams()) {
            for (StreamComponent component : stream.getAllComponents()) {
                if (component.getRepositoryURL().equals(repositoryURL) && component.getCodebase().equals(codebase)) {
                    streams.add(stream);
                    break; // Go to next stream
                }
            }
        }
        return streams;
    }

    @Override
    public StreamComponent getComponentBy(URI repositoryURL, Codebase codebase) {
        for (Stream stream : getStreams()) {
            for (StreamComponent component : stream.getAllComponents()) {
                if (component.getRepositoryURL().equals(repositoryURL) && component.getCodebase().equals(codebase)) {
                    return component;
                }
            }
        }
        return null;
    }
}
