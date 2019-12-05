/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.set.aphrodite.config;

import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;

import static org.jboss.set.aphrodite.config.AphroditeConfigTestUtils.assertDeepEqualsIssueConfig;
import static org.jboss.set.aphrodite.config.AphroditeConfigTestUtils.assertDeepEqualsRepositoryConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
public class AphroditeConfigJSONTest {

    public static final String JSON_FILE_PROPERTY = "aphrodite.config";
    public static final String VALID_JSON = "/test.aphrodite.properties.json";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private JsonReader jr;
    private IssueTrackerConfig jiraConfig;
    private RepositoryConfig githubConfig;

    @Before
    public void setUp() throws FileNotFoundException {
        String propertyFile = getClass().getResource(VALID_JSON).getPath();
        System.setProperty(JSON_FILE_PROPERTY, propertyFile);

        jr = Json.createReader(new FileInputStream(propertyFile));
        jiraConfig = new IssueTrackerConfig("https://issues.redhat.com/", "user", "pass", TrackerType.JIRA, 5);
        githubConfig = new RepositoryConfig("https://github.com/", "user", "pass", RepositoryType.GITHUB);
    }

    @Test
    public void issueTrackerValidJSONTest() {

        AphroditeConfig result = AphroditeConfig.fromJson(jr.readObject());
        assertNotNull("cannot create configuration from valid JSON file", result);
        assertEquals("Max thread count invalid", 10, result.getThreadCount());
        assertNotNull("error reading issue config from valid JSON file", result.getIssueTrackerConfigs());
        assertNotNull("error reading issue config from valid JSON file", result.getIssueTrackerConfigs().get(0));

        IssueTrackerConfig issueConfig = result.getIssueTrackerConfigs().get(0);
        //assertDeepEqualsIssueConfig(jiraConfig, issueConfig);

    }

    @Test
    public void repositoryConfigValidJSONTest() {

        AphroditeConfig result = AphroditeConfig.fromJson(jr.readObject());
        assertNotNull("cannot create configuration from valid JSON file", result);
        assertNotNull("error reading repository config from valid JSON file", result.getRepositoryConfigs());
        assertNotNull("error reading repository config from valid JSON file", result.getRepositoryConfigs().get(0));

        RepositoryConfig repositoryConfig = result.getRepositoryConfigs().get(0);
        assertDeepEqualsRepositoryConfig(githubConfig, repositoryConfig);
    }

    @Test
    public void JSONWithoutIssueConfigTest() {
        expectedException.expect(NullPointerException.class);

        JsonObject jo = Json.createObjectBuilder()
                .add("repositoryConfigs", Json.createArrayBuilder().build())
                .build();

        AphroditeConfig.fromJson(jo);
    }

    @Test
    public void JSONWithoutRepositoryConfigTest() {
        expectedException.expect(NullPointerException.class);

        JsonObject jo = Json.createObjectBuilder()
                .add("issueTrackerConfigs", Json.createArrayBuilder().build())
                .build();

        AphroditeConfig.fromJson(jo);
    }

    @Test
    public void JSONWithEmptyConfigsTest() {
        JsonObject jo = Json.createObjectBuilder()
                .add("issueTrackerConfigs", Json.createArrayBuilder().build())
                .add("repositoryConfigs", Json.createArrayBuilder().build())
                .build();

        AphroditeConfig result = AphroditeConfig.fromJson(jo);
        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getIssueTrackerConfigs());
        assertEquals(Collections.emptyList(), result.getRepositoryConfigs());
    }

    @Test
    public void JSONWithoutPropertiesTest() {
        expectedException.expect(NullPointerException.class);

        JsonObject jo = Json.createObjectBuilder()
                .add("issueTrackerConfigs", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("url", "https://issues.redhat.com/"))
                        .build())
                .add("repositoryConfigs", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("url", "https://github.com/"))
                        .build())
                .build();

        AphroditeConfig.fromJson(jo);
    }
}
