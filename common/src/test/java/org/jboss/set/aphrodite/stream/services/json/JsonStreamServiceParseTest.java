package org.jboss.set.aphrodite.stream.services.json;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.StreamConfig;
import org.jboss.set.aphrodite.config.StreamType;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonStreamServiceParseTest {

    private URL url;
    private File tmpFile;
    private JsonStreamService jsonStreamService;

    @Before
    public void before() throws MalformedURLException, NotFoundException {
        this.url = new File("src/test/resources/streams.json").getAbsoluteFile().toURI().toURL();
        this.tmpFile = new File("src/test/resources/streams2.json").getAbsoluteFile();
        initStreamService(this.url);
    }

    private void initStreamService(URL url) throws NotFoundException {
        final List<StreamConfig> streamConfigs = new ArrayList<>();
        final StreamConfig streamConfig = new StreamConfig(url, StreamType.JSON);
        streamConfigs.add(streamConfig);
        final AphroditeConfig config = new AphroditeConfig(null, null, streamConfigs);
        this.jsonStreamService = new JsonStreamService();
        this.jsonStreamService.init(null, config);
    }

    @After
    public void after() {
        this.jsonStreamService = null;
        this.tmpFile.delete();
    }

    protected void testParsedValues(final StreamComponent toUse) throws URISyntaxException {
        List<Stream> streams = this.jsonStreamService.getStreams();
        Assert.assertEquals(2, streams.size());
        testStream(streams.get(0), "stream1", null,
                new StreamComponent[] {
                        toUse == null ? new StreamComponent("comp1", Arrays.asList("comp1@redhat.com"),
                                new URI("https://github.com/project1/comp1.git/"), new Codebase("master"), "1", "1.0",
                                "org.jboss.test1", "a") : toUse,
                        new StreamComponent("comp2", Arrays.asList("comp2@redhat.com"),
                                new URI("https://github.com/project2/comp2.git/"), new Codebase("2.x"), "2.1", "2.1",
                                "org.jboss.test2", "b") });
        testStream(streams.get(1), "stream2", "stream1",
                new StreamComponent[] {
                        new StreamComponent("comp1", Arrays.asList("comp1@redhat.com"),
                                new URI("https://github.com/project1/comp1.git/"), new Codebase("master"), "1", "1.0",
                                "org.jboss.test1", "a"),
                        new StreamComponent("comp2", Arrays.asList("comp2@redhat.com"),
                                new URI("https://github.com/project2/comp2.git/"), new Codebase("2.x"), "2.1", "2.1",
                                "org.jboss.test2", "b") });
    }

    @Test
    public void testParsedValues() throws URISyntaxException {
        testParsedValues(null);
    }

    @Test
    public void testWithWrite() throws Exception {
        this.jsonStreamService.serializeStreams(this.url, new FileOutputStream(this.tmpFile));
        Assert.assertEquals(true, this.tmpFile.exists());
        this.initStreamService(this.tmpFile.toURI().toURL());
        // run regular tests
        testParsedValues();
    }

    @Test
    public void testWithUpdate() throws Exception {
        List<Stream> streams = this.jsonStreamService.getStreams();
        Assert.assertEquals(2, streams.size());
        final Stream stream = streams.get(0);
        final StreamComponent streamComponent = stream.getComponent("comp1");
        streamComponent.setTag("1.0-redhat-13");
        this.jsonStreamService.updateStreamComponent(streamComponent);
        this.jsonStreamService.serializeStreams(this.url, new FileOutputStream(this.tmpFile));
        this.initStreamService(this.tmpFile.toURI().toURL());
        testParsedValues(streamComponent);
    }

    private void testStream(final Stream stream, final String name, final String upstream, final StreamComponent[] comps) {
        Assert.assertEquals(name, stream.getName());
        Assert.assertEquals(upstream, stream.getUpstream() == null ? null : stream.getUpstream().getName());
        final Collection<StreamComponent> streamComponents = stream.getAllComponents();
        final Iterator<StreamComponent> whatWeGot = streamComponents.iterator();
        Assert.assertEquals(comps.length, streamComponents.size());
        for (int index = 0; index < comps.length; index++) {
            testStreamComponent(comps[index], whatWeGot.next());
        }
    }

    private void testStreamComponent(final StreamComponent expected, final StreamComponent whatWeGot) {
        Assert.assertEquals(expected.getName(), whatWeGot.getName());
        Assert.assertEquals(expected.getContacts(), whatWeGot.getContacts());
        Assert.assertEquals(expected.getRepositoryURL(), whatWeGot.getRepositoryURL());
        Assert.assertEquals(expected.getRepositoryType(), whatWeGot.getRepositoryType());
        Assert.assertEquals(expected.getCodebase(), whatWeGot.getCodebase());
        Assert.assertEquals(expected.getTag(), whatWeGot.getTag());
        Assert.assertEquals(expected.getVersion(), whatWeGot.getVersion());
        Assert.assertEquals(expected.getGAV(), whatWeGot.getGAV());
        Assert.assertEquals(expected.getComment(), whatWeGot.getComment());
    }
}
