package org.jboss.set.aphrodite.repository.services.github;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

/**
 * @author RyanEmerson
 */
class CustomGitHubClient extends GitHubClient {

    public static CustomGitHubClient createClient(String url) {
        try {
            String host = new URL(url).getHost();
            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
                host = HOST_API;
            return new CustomGitHubClient(host);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public CustomGitHubClient(String hostname) {
        super(hostname);
    }

    /**
     * Delete resource at URI. This method will throw an {@link IOException}
     * when the response status is not a 200 (OK) or 204 (No Content).
     *
     * Required for https://github.com/jboss-set/aphrodite/issues/59
     * GH returns an empty array if a label contains a space
     *
     * @param uri
     * @throws IOException
     */
    public void deleteWith200Response(final String uri)
            throws IOException {
        HttpURLConnection request = createDelete(uri);
        final int code = request.getResponseCode();
        updateRateLimits(request);
        if (code != 200 && code != 204)
            throw new RequestException(parseError(getStream(request)), code);
    }
}
