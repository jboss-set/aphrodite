package org.jboss.set.aphrodite.spi;

import java.net.URL;
import java.util.List;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;

public interface StreamService {

    /**
     * initial the streams service with the default url.
     * @throws NotFoundException
     */
    boolean init(Aphrodite aphrodite, AphroditeConfig config) throws NotFoundException;

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

    /**
     * Retrieve the URLs of all Repositories across all Streams
     * @return a list of unique Repository URLs
     */
    List<URL> getAllRepositoryURLs();

    /**
     * Retrieve the URLs of all Repositories associated with a given Stream.
     *
     * @param streamName the name of the <code>Stream</code> containing the returned repositories.
     * @return a list of unique Repository URLs
     */
    List<URL> getRepositoryURLsByStream(String streamName);

    /**
     * Find all the streams associated to the given repository and codebase
     * @param repository the <code>Repository</code> to search against.
     * @param codebase the <code>Codebase</code> to search against.
     * @return a list of named <code>Stream</code> objects.
     */
    List<Stream> getStreamsBy(Repository repository, Codebase codebase);

    /**
     * Get the component name based on the given repository and codebase.
     * @param repository the Repository to be searched against
     * @param codebase the codebase to be searched against
     * @return the name of the component of this repository. If it does not exist it will return the URL of the repository.
     */
    String getComponentNameBy(Repository repository, Codebase codebase);
}
