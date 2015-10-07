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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * @author Ryan Emerson
 */
public class AphroditeConfig {
    private final List<IssueTrackerConfig> issueTrackerConfigs;
    private final List<RepositoryConfig> repositoryConfigs;

    public AphroditeConfig(List<IssueTrackerConfig> issueTrackerConfigs, List<RepositoryConfig> repositoryConfigs) {
        this.issueTrackerConfigs = issueTrackerConfigs;
        this.repositoryConfigs = repositoryConfigs;
    }

    public AphroditeConfig(AphroditeConfig config) {
        this(new ArrayList<>(config.getIssueTrackerConfigs()), new ArrayList<>(config.getRepositoryConfigs()));
    }

    public List<IssueTrackerConfig> getIssueTrackerConfigs() {
        return issueTrackerConfigs;
    }

    public List<RepositoryConfig> getRepositoryConfigs() {
        return repositoryConfigs;
    }

    @Override
    public String toString() {
        return "AphroditeConfig{" +
                "issueTrackerConfigs=" + issueTrackerConfigs +
                ", repositoryConfigs=" + repositoryConfigs +
                '}';
    }

    public static AphroditeConfig fromJson(JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.getJsonArray("issueTrackerConfigs");
        Objects.requireNonNull(jsonArray, "issueTrackerConfigs array must be specified");
        List<IssueTrackerConfig> issueTrackerConfigs = jsonArray
                .stream()
                .map(JsonObject.class::cast)
                .map(json -> new IssueTrackerConfig(
                        json.getString("url", null),
                        json.getString("username", null),
                        json.getString("password", null),
                        json.getString("tracker", null)))
                .collect(Collectors.toList());

        jsonArray = jsonObject.getJsonArray("repositoryConfigs");
        Objects.requireNonNull(jsonArray, "repositoryConfigs array must be specified");
        List<RepositoryConfig> repositoryConfigs = jsonArray
                .stream()
                .map(JsonObject.class::cast)
                .map(json ->
                        new RepositoryConfig(
                                json.getString("url", null),
                                json.getString("username", null),
                                json.getString("password", null),
                                json.getString("type", null)))
                .collect(Collectors.toList());

        return new AphroditeConfig(issueTrackerConfigs, repositoryConfigs);
    }
}
