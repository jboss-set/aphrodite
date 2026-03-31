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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonReader;

import org.jboss.set.aphrodite.expr.AbstractPropertyExpressionResolver;
import org.jboss.set.aphrodite.expr.ExpressionResolver;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AphroditeConfigExprJSONTest {

    public static final String JSON_FILE_PROPERTY = "aphrodite.config";
    public static final String VALID_JSON = "/expr.aphrodite.properties.json";

    @Test
    public void testConfigWithExpressions() throws FileNotFoundException {
        String propertyFile = getClass().getResource(VALID_JSON).getPath();
        JsonReader jr = Json.createReader(new FileInputStream(propertyFile));
        ExpressionResolver resolver = new AbstractPropertyExpressionResolver() {
            
            @Override
            public String resolve(String expression) {
                Function<String, String> resolverTest = key -> switch (key) {
                    case "GH_USR" -> "user_gh";
                    case "GH_PWD" -> "pwd_gh";
                    case "RH_USR" -> "user_rh";
                    case "RH_PWD" -> "pwd_rh";
                    default -> "UNKNOWN";
                };
                return resolve(resolverTest, expression);
            }
        };
        AphroditeConfig result = AphroditeConfig.fromJson(resolver, jr.readObject());
        IssueTrackerConfig tracker = result.getIssueTrackerConfigs().get(0);
        assertEquals(tracker.getUsername(), "user_rh");
        assertEquals(tracker.getPassword(), "pwd_rh");

        RepositoryConfig repository = result.getRepositoryConfigs().get(0);
        assertEquals(repository.getUsername(), "user_gh");
        assertEquals(repository.getPassword(), "pwd_gh");

    }

}
