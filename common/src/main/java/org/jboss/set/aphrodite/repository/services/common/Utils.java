/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.repository.services.common;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wangc
 *
 */
public class Utils {

    private static final Pattern RELATED_PR_PATTERN = Pattern
            .compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN = Pattern.compile("([a-zA-Z_0-9-//]*)#(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO = Pattern
            .compile("([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)#(\\d+)", Pattern.CASE_INSENSITIVE);

    public static String createFromUrl(URL url) {
        return url != null ? createFromId(url.getPath()) : null;
    }

    private static String createFromId(String id) {
        if (id == null || id.length() == 0)
            return null;
        String owner = null;
        String name = null;
        for (String segment : id.split("/")) //$NON-NLS-1$
            if (segment.length() > 0)
                if (owner == null)
                    owner = segment;
                else if (name == null)
                    name = segment;
                else
                    break;
        return owner != null && owner.length() > 0 && name != null && name.length() > 0 ? owner + "/" + name : null;
    }

    public static List<URL> getPRFromDescription(URL url, String content) throws MalformedURLException, URISyntaxException {
        String[] paths = url.getPath().split("/");
        Matcher matcher = RELATED_PR_PATTERN.matcher(content);
        List<URL> relatedPullRequests = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                URL relatedPullRequest = new URI(
                        "https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pulls/" + matcher.group(3))
                                .toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        Matcher abbreviatedMatcher = ABBREVIATED_RELATED_PR_PATTERN.matcher(content);
        while (abbreviatedMatcher.find()) {
            String match = abbreviatedMatcher.group();
            Matcher abbreviatedExternalMatcher = ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO.matcher(match);
            if (abbreviatedExternalMatcher.find()) {
                if (abbreviatedExternalMatcher.groupCount() == 3) {
                    URL relatedPullRequest = new URI("https://github.com/" + abbreviatedExternalMatcher.group(1) + "/"
                            + abbreviatedExternalMatcher.group(2) + "/pull/" + abbreviatedExternalMatcher.group(3)).toURL();
                    relatedPullRequests.add(relatedPullRequest);
                    continue;
                }
            }

            if (abbreviatedMatcher.groupCount() == 2) {
                URL relatedPullRequest = new URI(
                        "https://github.com/" + paths[1] + "/" + paths[2] + "/" + "/pulls/" + abbreviatedMatcher.group(2))
                                .toURL();
                relatedPullRequests.add(relatedPullRequest);
            }
        }
        return relatedPullRequests;
    }
}
