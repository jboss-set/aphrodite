package org.jboss.set.aphrodite.domain.internal;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class URLUtilsTest {

    private static String UPSTREAM_PR = "Upstream PR:";
    private static String SIMPLE_URL = "http://issues.redhat.com/";
    private static String SHORT_URL = "ftp://jboss.org";
    private static String URL_WITH_CREDENTIALS = "https://tom:smurfs@issues.redhat.com";
    private static String URL_WITH_PARAMETERS = "https://issues.redhat.com/browse/?filter=-1&true=true";
    private static String URL_ALL_IN = "https://tom:smurfs@issues.redhat.com/browse/?filter=-1&true=true";
    private static String URL_IPV4 = "https://1.2.3.4/a/b/c";
    private static String INVALID_URL_MISSING_PROTO = "www.jboss.org";
    private static String INVALID_URL_TOO_SHORT = "http://www";
    private static String INVALID_URL_DASH_AT_THE_END = "http://www.jboss-.org";
    private static String INVALID_URL_DOT_AT_THE_END = "http://www.jboss.org.";

    /**
     * Tests URLUtils.URL_REGEX_STRING constant
     */
    @Test
    public void testUrlRegexConstant() {
        Pattern initialPattern = Pattern.compile(URLUtils.URL_REGEX_STRING);

        Assert.assertTrue(initialPattern.matcher(SIMPLE_URL).matches());
        Assert.assertTrue(initialPattern.matcher(SHORT_URL).matches());
        Assert.assertTrue(initialPattern.matcher(URL_WITH_CREDENTIALS).matches());
        Assert.assertTrue(initialPattern.matcher(URL_WITH_PARAMETERS).matches());
        Assert.assertTrue(initialPattern.matcher(URL_ALL_IN).matches());
        Assert.assertTrue(initialPattern.matcher(URL_IPV4).matches());

        Assert.assertFalse(initialPattern.matcher(INVALID_URL_MISSING_PROTO).matches());
        Assert.assertFalse(initialPattern.matcher(INVALID_URL_TOO_SHORT).matches());
        Assert.assertFalse(initialPattern.matcher(INVALID_URL_DASH_AT_THE_END).matches());
        Assert.assertFalse(initialPattern.matcher(INVALID_URL_DOT_AT_THE_END).matches());
    }

    @Test
    public void testExtractSingleUrl() {
        String initialExp = UPSTREAM_PR + URLUtils.URL_REGEX_STRING;
        final String text = "Some text, " + UPSTREAM_PR;
        assertMatch(initialExp, text + SIMPLE_URL, SIMPLE_URL);
        assertMatch(initialExp, text + SHORT_URL, SHORT_URL);
        assertMatch(initialExp, text + URL_WITH_PARAMETERS, URL_WITH_PARAMETERS);
        assertMatch(initialExp, text + URL_WITH_CREDENTIALS, URL_WITH_CREDENTIALS);
        assertMatch(initialExp, text + URL_ALL_IN, URL_ALL_IN);
    }

    @Test
    public void testExtractMultipleUrls() {
        String initialExp = UPSTREAM_PR + URLUtils.URL_REGEX_STRING + ",(" + URLUtils.URL_REGEX_STRING + ")*";
        final String text = "Some text, " + UPSTREAM_PR + SIMPLE_URL + "," + URL_WITH_PARAMETERS;

        Pattern initialPattern = Pattern.compile(initialExp);
        String[] urls = URLUtils.extractURLs(text, initialPattern, true);
        Assert.assertNotNull(urls);
        Assert.assertEquals(2, urls.length);
        Assert.assertEquals(SIMPLE_URL, urls[0]);
        Assert.assertEquals(URL_WITH_PARAMETERS, urls[1]);
    }

    private void assertMatch(String initialRegexp, String text, String expectedUrl) {
        Pattern initialPattern = Pattern.compile(initialRegexp);
        String[] urls = URLUtils.extractURLs(text, initialPattern, false);
        Assert.assertNotNull(urls);
        Assert.assertEquals(1, urls.length);
        Assert.assertEquals(expectedUrl, urls[0]);
    }
}
