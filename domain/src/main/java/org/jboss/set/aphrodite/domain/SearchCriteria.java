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

package org.jboss.set.aphrodite.domain;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * A generic search object that allows for issues to be searched without knowing details of the
 * underlying issue tracking system.
 *
 * @author Ryan Emerson
 */
public class SearchCriteria {
    private final String status;
    private final String assignee;
    private final String reporter;
    private final String product;
    private final String component;
    private final Stage stage;
    private final Release release;
    private final Map<Stream, FlagStatus> streams;
    private final LocalDate lastUpdated;
    private final Integer maxResults;

    private SearchCriteria(String status, String assignee, String reporter, String product,
            String component, Stage stage, Release release, Map<Stream, FlagStatus> streams,
            LocalDate lastUpdated, Integer maxResults) {
        this.status = status;
        this.assignee = assignee;
        this.reporter = reporter;
        this.product = product;
        this.component = component;
        this.stage = stage;
        this.release = release;
        this.streams = streams;
        this.lastUpdated = lastUpdated;
        this.maxResults = maxResults;

        if (lastUpdated != null && lastUpdated.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("lastUpdated cannot be in the future.");
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public Optional<String> getAssignee() {
        return Optional.ofNullable(assignee);
    }

    public Optional<String> getReporter() {
        return Optional.ofNullable(reporter);
    }

    public Optional<String> getProduct() {
        return Optional.ofNullable(product);
    }

    public Optional<String> getComponent() {
        return Optional.ofNullable(component);
    }

    public Optional<Stage> getStage() {
        return Optional.ofNullable(stage);
    }

    public Optional<Release> getRelease() {
        return Optional.ofNullable(release);
    }

    public Optional<Map<Stream, FlagStatus>> getStreams() {
        return Optional.ofNullable(streams);
    }

    public Optional<LocalDate> getLastUpdated() {
        return Optional.ofNullable(lastUpdated);
    }

    public Optional<Integer> getMaxResults() {
        return Optional.ofNullable(maxResults);
    }

    public boolean isEmpty() {
        return status == null && assignee == null && reporter == null && product == null && component == null && stage == null
                && release == null && streams == null && lastUpdated == null && maxResults == null;
    }

    public static class Builder {

        private String status;
        private String assignee;
        private String reporter;
        private String product;
        private String component;
        private Stage stage;
        private Release release;
        private Map<Stream, FlagStatus> streams;
        private LocalDate startDate;
        private Integer maxResults;

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder setAssignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        public Builder setReporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        public Builder setProduct(String product) {
            this.product = product;
            return this;
        }

        public Builder setComponent(String component) {
            this.component = component;
            return this;
        }

        public Builder setStage(Stage stage) {
            this.stage = stage;
            return this;
        }

        public Builder setRelease(Release release) {
            this.release = release;
            return this;
        }

        public Builder setStreams(Map<Stream, FlagStatus> streams) {
            this.streams = streams;
            return this;
        }

        public Builder setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(status, assignee, reporter, product, component, stage, release,
                    streams, startDate, maxResults);
        }
    }
}
