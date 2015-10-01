package org.jboss.set.aphrodite.spi;

import java.util.List;

import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamConfiguration;

public interface StreamService {

    List<Stream> getStreams();

    StreamConfiguration getConfigurationFor(Stream stream);

}
