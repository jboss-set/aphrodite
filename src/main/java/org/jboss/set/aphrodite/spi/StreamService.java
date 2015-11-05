package org.jboss.set.aphrodite.spi;

import org.jboss.set.aphrodite.domain.Stream;

import java.util.List;

public interface StreamService {

    List<Stream> getStreams();

    Stream getStream(String streamName);

}
