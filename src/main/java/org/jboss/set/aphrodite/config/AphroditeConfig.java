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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.set.aphrodite.repository.services.common.RepositoryType;

/**
 * @author Ryan Emerson
 */
public class AphroditeConfig {
    private final ExecutorService executorService;
    private final List<IssueTrackerConfig> issueTrackerConfigs;
    private final List<RepositoryConfig> repositoryConfigs;
    private final List<StreamConfig> streamConfigs;

    public static AphroditeConfig singleIssueTracker(IssueTrackerConfig issueTrackerConfig) {
        List<IssueTrackerConfig> list = new ArrayList<>();
        list.add(issueTrackerConfig);
        return issueTrackersOnly(list);
    }

    public static AphroditeConfig issueTrackersOnly(List<IssueTrackerConfig> issueTrackerConfigs) {
        return new AphroditeConfig(issueTrackerConfigs, new ArrayList<>(), new ArrayList<>());
    }

    public static AphroditeConfig singleRepositoryService(RepositoryConfig repositoryConfig) {
        List<RepositoryConfig> list = new ArrayList<>();
        list.add(repositoryConfig);
        return repositoryServicesOnly(list);
    }

    public static AphroditeConfig repositoryServicesOnly(List<RepositoryConfig> repositoryConfigs) {
        return new AphroditeConfig(new ArrayList<>(), repositoryConfigs, new ArrayList<>());
    }

    public AphroditeConfig(List<IssueTrackerConfig> issueTrackerConfigs, List<RepositoryConfig> repositoryConfigs, List<StreamConfig> streamConfigs) {
        this.issueTrackerConfigs = issueTrackerConfigs;
        this.repositoryConfigs = repositoryConfigs;
        this.streamConfigs = streamConfigs;
        this.executorService = Executors.newCachedThreadPool();
    }

    public AphroditeConfig(ExecutorService executorService,
            List<IssueTrackerConfig> issueTrackerConfigs,
            List<RepositoryConfig> repositoryConfigs,
            List<StreamConfig> streamConfigs) {
        this.executorService = executorService;
        this.issueTrackerConfigs = issueTrackerConfigs;
        this.repositoryConfigs = repositoryConfigs;
        this.streamConfigs = streamConfigs;
    }

    public AphroditeConfig(AphroditeConfig config) {
        this(config.getExecutorService(), new ArrayList<>(config.getIssueTrackerConfigs()),
                new ArrayList<>(config.getRepositoryConfigs()), new ArrayList<>(config.getStreamConfigs()));
    }


    public ExecutorService getExecutorService() {
        return executorService;
    }

    public List<IssueTrackerConfig> getIssueTrackerConfigs() {
        return issueTrackerConfigs;
    }

    public List<RepositoryConfig> getRepositoryConfigs() {
        return repositoryConfigs;
    }

    public List<StreamConfig> getStreamConfigs(){
        return streamConfigs;
    }

    @Override
    public String toString() {
        return "AphroditeConfig{" +
                "issueTrackerConfigs=" + issueTrackerConfigs +
                ", repositoryConfigs=" + repositoryConfigs +
                ",streamConfigs=" + streamConfigs+
                '}';
    }

    public static AphroditeConfig fromJson(JsonObject jsonObject) {
        int maxThreadCount = jsonObject.getInt("maxThreadCount", 0);

        JsonArray jsonArray = jsonObject.getJsonArray("issueTrackerConfigs");
        Objects.requireNonNull(jsonArray, "issueTrackerConfigs array must be specified");
        List<IssueTrackerConfig> issueTrackerConfigs = jsonArray
                .stream()
                .map(JsonObject.class::cast)
                .map(json -> new IssueTrackerConfig(
                        json.getString("url", null),
                        json.getString("username", null),
                        json.getString("password", null),
                        TrackerType.valueOf(json.getString("tracker", null)),
                        json.getInt("defaultIssueLimit", -1)))
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
                                RepositoryType.valueOf(json.getString("type", null))))
                .collect(Collectors.toList());

        if (maxThreadCount > 0)
            return new AphroditeConfig(Executors.newFixedThreadPool(maxThreadCount), issueTrackerConfigs,repositoryConfigs,null);

        // IF maxThreadCount has not been specified, then we refer to the default executorService which is an unlimited cachedThreadPool
        return new AphroditeConfig(issueTrackerConfigs, repositoryConfigs,null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AphroditeConfig that = (AphroditeConfig) o;

        if (issueTrackerConfigs != null ? !issueTrackerConfigs.equals(that.issueTrackerConfigs) : that.issueTrackerConfigs != null)
            return false;
        if (repositoryConfigs != null ? !repositoryConfigs.equals(that.repositoryConfigs) : that.repositoryConfigs != null)
            return false;
        return streamConfigs != null ? streamConfigs.equals(that.streamConfigs) : that.streamConfigs == null;

    }

    @Override
    public int hashCode() {
        int result = issueTrackerConfigs != null ? issueTrackerConfigs.hashCode() : 0;
        result = 31 * result + (repositoryConfigs != null ? repositoryConfigs.hashCode() : 0);
        result = 31 * result + (streamConfigs != null ? streamConfigs.hashCode() : 0);
        return result;
    }
}
