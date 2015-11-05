package org.jboss.set.aphrodite.spi;

import org.jboss.set.aphrodite.domain.Stream;

import java.util.List;

public interface StreamService {

    /**
     * Returns all streams discovered by this service.
     *
     * @return a list of all streams discovered by this <code>StreamService</code>
     */
    List<Stream> getStreams();

    /**
     * Get a specific <code>Stream</code> object based upon its String name.
     *
     * @param streamName the name of the <code>Stream</code> to be returned.
     * @return Stream the <code>Stream</code> object which corresponds to the specified streamName
     *                if it exists, otherwise null.
     */
    Stream getStream(String streamName);

}
