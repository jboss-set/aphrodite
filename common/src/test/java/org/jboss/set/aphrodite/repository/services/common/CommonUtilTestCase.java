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

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author wangc
 *
 */
public class CommonUtilTestCase {
    
    private static String TEST_URL = "https://github.com/jboss-set/aphrodite/pull/100";
    private static String TEST_URL_WITH_SLASH = "https://github.com/jboss-set/aphrodite/pull/100/";
    private static String TEST_REPOSITORYID = "jboss-set/aphrodite";

    private static String PR_URL = "https://github.com/jbossas/jboss-eap7/pull/2499";
    private static String REFERENCED_PR_URL = "https://github.com/jbossas/jboss-eap7/pull/100";
    private static String REFERENCED_PR_URL_EXTERNAL = "https://github.com/wildfly/wildfly/pull/10611";
    private static String DESC = "(cherry picked from commit 9d0bd0a68656a6e859f3c5742ab547a3b4a3053f)\n" + "\n"
            + "GSS 7.1.z issue: https://issues.jboss.org/browse/JBEAP-13660\n" + "\n"
            + "upstream 7.2.0 issue: 7.2.0 https://issues.jboss.org/browse/JBEAP-13659\n"
            + "upstream WFLY issue: https://issues.jboss.org/browse/WFLY-9499\n" + "\n" + "upstream PR: wildfly/wildfly#10611";

    private static String DESC_WITH_EXTERNAL_PR = "(cherry picked from commit 9d0bd0a68656a6e859f3c5742ab547a3b4a3053f)\n"
            + "\n" + "GSS 7.1.z issue: https://issues.jboss.org/browse/JBEAP-13660\n" + "\n"
            + "upstream 7.2.0 issue: 7.2.0 https://issues.jboss.org/browse/JBEAP-13659\n"
            + "upstream WFLY issue: https://issues.jboss.org/browse/WFLY-9499\n" + "\n" + "upstream PR: jbossas/jboss-eap7#100";

    @Test
    public void testCreateFromUrl() {
        try {
            String result = RepositoryUtils.createRepositoryIdFromUrl(new URL(TEST_URL));
            Assert.assertEquals("repository id don't match", TEST_REPOSITORYID, result);
        } catch (MalformedURLException e) {
            fail("MalformedURLException should not happen in test");
        }
        
        try {
            String result = RepositoryUtils.createRepositoryIdFromUrl(new URL(TEST_URL_WITH_SLASH));
            Assert.assertEquals("repository id don't match", TEST_REPOSITORYID, result);
        } catch (MalformedURLException e) {
            fail("MalformedURLException should not happen in test");
        }
    }

    @Test
    public void testGetPRFromDescription() {
        try {
            URL url = new URL(PR_URL);
            List<URL> pullRequests = RepositoryUtils.getPRFromDescription(url, DESC);
            Assert.assertEquals("Incorrect number of PR from description ", 1, pullRequests.size());
            Assert.assertEquals("Incorrect PR from description ", REFERENCED_PR_URL_EXTERNAL, pullRequests.get(0).toString());
        } catch (MalformedURLException | URISyntaxException e) {
            fail("Exception should not happen in test");
        }
        
        try {
            URL url = new URL(PR_URL);
            List<URL> pullRequests = RepositoryUtils.getPRFromDescription(url, DESC_WITH_EXTERNAL_PR);
            Assert.assertEquals("Incorrect number of PR from description ", 1, pullRequests.size());
            Assert.assertEquals("Incorrect PR from description ", REFERENCED_PR_URL, pullRequests.get(0).toString());
        } catch (MalformedURLException | URISyntaxException e) {
            fail("Exception should not happen in test");
        }
    }
}
