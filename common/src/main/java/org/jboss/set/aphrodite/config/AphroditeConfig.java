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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;

/**
 * @author Ryan Emerson
 */
public class AphroditeConfig {
    private static final int DEFAULT_STREAM_SERVICE_UPDATE_RATE = 0; // default to 0 means no auto stream update
    private static final int DEFAULT_INITIAL_DELAY = 0; // default to 0 means no initial delay
    private static final int DEFAULT_MAX_THREADS = 2;
    private final ScheduledExecutorService executorService;
    private final List<IssueTrackerConfig> issueTrackerConfigs;
    private final List<RepositoryConfig> repositoryConfigs;
    private final List<StreamConfig> streamConfigs;
    private final int streamServiceUpdateRate;
    private final int initialDelay;
    private int threadCount;

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (!t.isDaemon())
                t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

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
       this(Executors.newScheduledThreadPool(DEFAULT_MAX_THREADS, new DefaultThreadFactory()), issueTrackerConfigs, repositoryConfigs, streamConfigs, DEFAULT_STREAM_SERVICE_UPDATE_RATE, DEFAULT_INITIAL_DELAY,DEFAULT_MAX_THREADS);
    }

    public AphroditeConfig(List<IssueTrackerConfig> issueTrackerConfigs, List<RepositoryConfig> repositoryConfigs, List<StreamConfig> streamConfigs, int streamServiceUpdateRate, int initialDelay) {
       this(Executors.newScheduledThreadPool(DEFAULT_MAX_THREADS, new DefaultThreadFactory()), issueTrackerConfigs, repositoryConfigs, streamConfigs, streamServiceUpdateRate, initialDelay,DEFAULT_MAX_THREADS);
    }

    public AphroditeConfig(List<IssueTrackerConfig> issueTrackerConfigs, List<RepositoryConfig> repositoryConfigs, List<StreamConfig> streamConfigs, int streamServiceUpdateRate, int initialDelay, int maxThreads) {
       this(Executors.newScheduledThreadPool(maxThreads, new DefaultThreadFactory()), issueTrackerConfigs, repositoryConfigs, streamConfigs, streamServiceUpdateRate, initialDelay,maxThreads);
    }

    public AphroditeConfig(ScheduledExecutorService executorService, List<IssueTrackerConfig> issueTrackerConfigs,
            List<RepositoryConfig> repositoryConfigs, List<StreamConfig> streamConfigs, int streamServiceUpdateRate,
            int initialDelay, int threadCount) {
        super();
        this.executorService = executorService;
        this.issueTrackerConfigs = issueTrackerConfigs;
        this.repositoryConfigs = repositoryConfigs;
        this.streamConfigs = streamConfigs;
        this.streamServiceUpdateRate = streamServiceUpdateRate;
        this.initialDelay = initialDelay;
        this.threadCount = threadCount;
    }

    public AphroditeConfig(AphroditeConfig config) {
        this(config.getExecutorService(), new ArrayList<>(config.getIssueTrackerConfigs()), new ArrayList<>(config.getRepositoryConfigs()), new ArrayList<>(config.getStreamConfigs()), config.getStreamServiceUpdateRate(), config.getInitialDelay(), config.getThreadCount());
    }

    public AphroditeConfig(List<IssueTrackerConfig> issueTrackerConfigs, List<RepositoryConfig> repositoryConfigs,
            List<StreamConfig> streamConfigs, int maxThreadCount) {
        this(Executors.newScheduledThreadPool(DEFAULT_MAX_THREADS, new DefaultThreadFactory()), issueTrackerConfigs, repositoryConfigs, streamConfigs, DEFAULT_STREAM_SERVICE_UPDATE_RATE, DEFAULT_INITIAL_DELAY, DEFAULT_MAX_THREADS);
    }

    public ScheduledExecutorService getExecutorService() {
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

    public int getStreamServiceUpdateRate() {
        return streamServiceUpdateRate;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public static AphroditeConfig fromJson(JsonObject jsonObject) {
        int maxThreadCount = jsonObject.getInt("maxThreadCount", DEFAULT_MAX_THREADS);
        int streamServiceUpdateRate = jsonObject.getInt("streamServiceUpdateRate", DEFAULT_STREAM_SERVICE_UPDATE_RATE);
        int initialDelay = jsonObject.getInt("initialDelay", DEFAULT_INITIAL_DELAY);

        List<IssueTrackerConfig> issueTrackerConfigs = getIssueTrackerConfigs(jsonObject);
        List<RepositoryConfig> repositoryConfigs = getRepositoryConfigs(jsonObject);
        List<StreamConfig> streamConfigs = getStreamConfigs(jsonObject);

        return new AphroditeConfig(Executors.newScheduledThreadPool(maxThreadCount, new DefaultThreadFactory()), issueTrackerConfigs,
                    repositoryConfigs, streamConfigs, streamServiceUpdateRate, initialDelay, maxThreadCount);
    }

    private static List<IssueTrackerConfig> getIssueTrackerConfigs(JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.getJsonArray("issueTrackerConfigs");
        Objects.requireNonNull(jsonArray, "issueTrackerConfigs array must be specified");
        return jsonArray.stream()
                .map(JsonObject.class::cast)
                .map(json -> new IssueTrackerConfig(
                        json.getString("url", null),
                        json.getString("username", null),
                        json.getString("password", null),
                        TrackerType.valueOf(json.getString("tracker", null)),
                        json.getInt("defaultIssueLimit", -1)))
                .collect(Collectors.toList());
    }

    private static List<RepositoryConfig> getRepositoryConfigs(JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.getJsonArray("repositoryConfigs");
        Objects.requireNonNull(jsonArray, "repositoryConfigs array must be specified");
        return jsonArray.stream()
                .map(JsonObject.class::cast)
                .map(json ->
                        new RepositoryConfig(
                                json.getString("url", null),
                                json.getString("username", null),
                                json.getString("password", null),
                                RepositoryType.valueOf(json.getString("type", null))))
                .collect(Collectors.toList());
    }

    private static List<StreamConfig> getStreamConfigs(JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.getJsonArray("streamConfigs");
        if (jsonArray == null)
            return new ArrayList<>();

        return jsonArray.stream()
                .map(JsonObject.class::cast)
                .map(json -> {
                    StreamType type = StreamType.valueOf(json.getString("type", null));
                    String url = json.getString("url", null);
                    String fileLocation = json.getString("file", null);

                    if (url != null && fileLocation != null)
                        throw new IllegalArgumentException("A StreamConfigs entry cannot contain both a 'file' and 'url' field");

                    if (url != null) {
                        return new StreamConfig(Utils.createURL(url), type);
                    }

                    if (fileLocation == null)
                        throw new IllegalArgumentException("A StreamConfigs entry must have a 'file' or 'url' field set");

                    return new StreamConfig(new File(fileLocation), type);
                }).collect(Collectors.toList());
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

    @Override
    public String toString() {
        return "AphroditeConfig{" +
                "issueTrackerConfigs=" + issueTrackerConfigs +
                ", repositoryConfigs=" + repositoryConfigs +
                ",streamConfigs=" + streamConfigs+
                '}';
    }
}
