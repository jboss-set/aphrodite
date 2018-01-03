/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.set.aphrodite.issue.trackers.jira;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.Version;

/**
 * @author wangc
 *
 */
public class JiraPayloadHomeTest {

    private static List<Version> versions;

    @BeforeClass
    public static void setup() {
        try {
            versions = Arrays.asList(
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.1.0.Alpha1", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.1.0.CR1", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.1.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.2.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.1.1.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.3.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "6.4.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.DR12", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.DR13 (Alpha)", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.ER2 (Beta)", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.ER7", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.CR2", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.3.CR3-doc", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.1.0.CR4", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.1.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.0.z.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.1.1.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.2.0.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.1.z.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.Doc.Test", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "Individual Patches.GA", "", false, true, new DateTime()),
                    new Version(new URI("https://issues.jboss.org/rest/api/2/version/12345678"), (long) 12345678, "7.backlog.GA", "", false, true, new DateTime())
                    );
        } catch (URISyntaxException e) {
            // ignore
        }
    }

    @Test
    public void testPayloadVersionFilter() {
        int counter = 0;
        for(Version version : versions) {
            if(JiraPayloadHome.filterPayloadVersion(version)) {
                counter++;
            }
        }
        assertEquals(4, counter);
    }

}
