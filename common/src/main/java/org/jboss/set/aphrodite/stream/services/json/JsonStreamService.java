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

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.StreamConfig;
import org.jboss.set.aphrodite.config.StreamType;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.domain.StreamComponentUpdateException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.spi.StreamService;

/**
 * A stream service which reads stream data from the specified JSON file. This implementation assumes that streams are written
 * in order in the json file, i.e. the most recent (upstream) issue is specified as the first JSON object in the "streams" JSON
 * array. An example JSON file can be found at https://github.com/jboss-set/jboss-streams
 *
 * @author Ryan Emerson
 * @author baranowb
 */
public class JsonStreamService implements StreamService {
    private static final Log LOG = LogFactory.getLog(JsonStreamService.class);

    private final Map<String, Stream> parsedStreamsMap = new LinkedHashMap<>();
    // DO NOT CHANGE THIS
    // this collection contain mapping of url to list of streams. Order of those MUST be retained
    // as on READ operation. We write from this structure, if order change, huge diff on small change
    // might happen. We store LinkedHashMap.values(), which retain order from map
    private final Map<URL, Collection<Stream>> urlToParsedStreams = new LinkedHashMap<>();
    private AphroditeConfig config;

    @Override
    public boolean init(Aphrodite aphrodite, AphroditeConfig config) throws NotFoundException {
        this.config = config;
        return updateStreams();
    }

    private boolean init(StreamConfig config) throws NotFoundException {
        URL url = null;
        if (config.getURL().isPresent()) {
            url = config.getURL().get();
        } else if (config.getStreamFile().isPresent()) {
            try {
                url = config.getStreamFile().get().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new NotFoundException(e);
            }
        } else {
            throw new NotFoundException("StreamConfig requires either a URL or File to be specified");
        }
        Map<String, Stream> streamsMap = StreamsJsonParser.parse(url);
        Set<String> toRetainMap = new HashSet<String>(streamsMap.keySet());
        toRetainMap.retainAll(parsedStreamsMap.keySet());
        if (toRetainMap.size() > 0) {
            throw new IllegalArgumentException(
                    "URL '" + url + "' contain entires that overlap: " + Arrays.toString(toRetainMap.toArray()));
        }
        this.parsedStreamsMap.putAll(streamsMap);
        this.urlToParsedStreams.put(url, streamsMap.values());
        return true;
    }

    @Override
    public synchronized boolean updateStreams() throws NotFoundException {
        Iterator<StreamConfig> i = this.config.getStreamConfigs().iterator();
        while (i.hasNext()) {
            StreamConfig streamConfig = i.next();
            if (streamConfig.getStreamType() == StreamType.JSON) {
                return init(streamConfig);
            }
        }
        return false;
    }

    @Override
    public synchronized List<Stream> getStreams() {
        return new ArrayList<>(parsedStreamsMap.values());
    }

    @Override
    public synchronized Stream getStream(String streamName) {
        return parsedStreamsMap.get(streamName);
    }

    @Override
    public List<URI> getDistinctURLRepositories() {
        return getStreams().stream().flatMap(stream -> getDistinctURLRepositoriesByStream(stream.getName()).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<URI> getDistinctURLRepositoriesByStream(String streamName) {
        Stream stream = getStream(streamName);
        if (stream == null)
            return new ArrayList<>();

        return stream.getAllComponents().stream().map(StreamComponent::getRepositoryURL).distinct()
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

    @Override
    public StreamComponent updateStreamComponent(StreamComponent streamComponent) throws StreamComponentUpdateException {
        // TODO, this possibly should be inner call StreamComponent->Stream->StreamService ?
        // This OP is  sanity OP, since right now implementation of Stream and Json service would work with out it
        // though it is good to have some check on what we push.
        Stream owner = streamComponent.getStream();
        if(owner == null){
            throw new StreamComponentUpdateException("No owner stream for component.", streamComponent);
        }
        // LinkedHashMap, DO NOT PERFORM REMOVE
        owner.updateComponent(streamComponent);
        return streamComponent;
    }

    @Override
    public void serializeStreams(URL url, OutputStream out) throws NotFoundException {
        final Collection<Stream> streams = this.urlToParsedStreams.get(url);
        if (streams == null) {
            throw new NotFoundException("No matching set of streams for '" + url + "'");
        }
        JsonObject jsonObject = StreamsJsonParser.encode(streams);
        // JsonWriter jsonWriter = Json.createWriter(out);
        // jsonWriter.writeObject(jsonObject);
        // jsonWriter.close();
        Map<String, Boolean> config = buildConfig();
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        JsonWriter jsonWriter = writerFactory.createWriter(out);
        jsonWriter.write(jsonObject);
        jsonWriter.close();
    }

    private static Map<String, Boolean> buildConfig() {
        final Map<String, Boolean> config = new HashMap<String, Boolean>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        return config;
    }
}