package org.jboss.set.aphrodite.domain.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public final class URLUtils {

    /**
     * Supports domain names and IPv4. Is more permissive than spec of course.
     *
     * TODO: IPv6 support.
     */
    public static final String URL_REGEX_STRING= "(http|ftp|https)://(\\w+(:\\w+)@)?([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9]+(/[\\w.@?^=%&:/~+#-]*)?";
    private static final Pattern URL_REGEX= Pattern.compile(URL_REGEX_STRING);

    // GitHub partial URL: <org>/<repo>#<pr-number>
    private static final Pattern GITHUB_PR_REGEX = Pattern.compile("(\\S+)/(\\S+)#(\\d+)");

    private URLUtils() {
    }

    /**
     * Extracts URLs matching given pattern from given String.
     *
     * @param source String to extract URLs from.
     * @param initialMatchPattern Pattern that URLs must match.
     * @param multiple If false, return the first URL only, if true, return all.
     * @return URLs matching <code>initialMatchPattern</code>
     */
    public static String[] extractURLs(final String source, final Pattern initialMatchPattern, final boolean multiple) {
        Matcher m = initialMatchPattern.matcher(source);
        if (m.find()) {
            final String urlSource = source.substring(m.start(), m.end());
            m = URL_REGEX.matcher(urlSource);
            if (multiple) {
                final List<String> urls = new ArrayList<>();
                while (m.find()) {
                    urls.add(urlSource.substring(m.start(), m.end()));
                }
                return urls.toArray(new String[urls.size()]);
            } else {
                // just to be thorough
                if (m.find()) {
                    return new String[] { urlSource.substring(m.start(), m.end()) };
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static String[] extractGHUrls(final String source, final Pattern initialMatchPattern, final boolean multiple) {
        Matcher m = initialMatchPattern.matcher(source);
        if (m.find()) {
            final String urlSource = source.substring(m.start(), m.end());
            m = GITHUB_PR_REGEX.matcher(urlSource);
            if (multiple) {
                final List<String> urls = new ArrayList<>();
                while (m.find()) {
                    urls.add(makeFullUrl(m));
                }
                return urls.toArray(new String[urls.size()]);
            } else {
                // just to be thorough
                if (m.find()) {
                    return new String[] { makeFullUrl(m) };
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static String makeFullUrl(Matcher m) {
        return "https://github.com/" + m.group(1) + "/" + m.group(2) + "/pull/" + m.group(3);
    }

}
