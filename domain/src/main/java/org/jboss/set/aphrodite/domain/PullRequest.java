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

import static org.jboss.set.aphrodite.domain.internal.URLUtils.URL_REGEX_STRING;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.set.aphrodite.domain.internal.URLUtils;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PullRequest {
    private static final Pattern UPGRADE_TITLE = Pattern.compile("\\s*Upgrade \\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSTREAM_ISSUE_NOT_REQUIRED = Pattern.compile("^\\s*Upstream not required.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSTREAM_PR = Pattern.compile("^\\s*Upstream PR[:|]\\s*+" + URL_REGEX_STRING, Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSTREAM_ISSUE = Pattern.compile("^\\s*Upstream Issue[:|]\\s*+"+URL_REGEX_STRING, Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUE = Pattern.compile("^\\s*Issue[:|]\\s*+"+URL_REGEX_STRING, Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATED_ISSUES = Pattern.compile("^\\s*Related Issue[s|][:|]\\s*+"+URL_REGEX_STRING+"(,\\s*+"+URL_REGEX_STRING+")*+", Pattern.CASE_INSENSITIVE);
    private static final String UPGRADE_META_BIT_REGEX = "\\w++=\\w++";
    private static final String UPGRADE_META_REGEX = "\\s*+"+UPGRADE_META_BIT_REGEX+"(,\\s*+"+UPGRADE_META_BIT_REGEX+")*+";
    private static final Pattern UPGRADE = Pattern.compile("\\s*Upgrade[:|]"+UPGRADE_META_REGEX, Pattern.CASE_INSENSITIVE);

    private final String id;
    private final URL url;
    private final Codebase codebase;
    private PullRequestState state;
    private String title;
    private String body;
    private Repository repository;
    private boolean mergeable, merged, upgrade;
    private MergeableState mergableState;
    private Date mergedAt;

    public PullRequest(String id, URL url, Repository repository, Codebase codebase, PullRequestState state, String title, String body, boolean mergeable, boolean merged, MergeableState mergeableState, Date mergedAt) {
        this.id = id;
        this.url = url;
        this.codebase = codebase;
        this.state = state;
        this.title = title;
        this.body = body;
        this.repository = repository;
        this.mergeable = mergeable;
        this.merged = merged;
        this.mergedAt = mergedAt;
        if(this.title != null)
            this.upgrade = UPGRADE_TITLE.matcher(this.title).find();
    }

    public String getId() {
        return id;
    }

    public URL getURL() {
        return url;

    }

    public Codebase getCodebase() {
        return codebase;
    }

    public PullRequestState getState() {
        return state;
    }

    public void setState(PullRequestState state) {
        this.state = state;
    }

    public String getTitle() {
        return title;

    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Checks if PR body contains indication that upstream work is not required.
     *
     * @return is upstream required?
     */
    public boolean isUpstreamRequired() {
        final Matcher m = UPSTREAM_ISSUE_NOT_REQUIRED.matcher(body);
        return !m.find();
    }

    /**
     * Searches PR body for link to upstream PR.
     *
     * @return upstream PR URL or null
     * @throws MalformedURLException if found URL is invalid
     */
    public URL findUpstreamPullRequestURL() throws MalformedURLException {
        if (this.isUpstreamRequired()) {
            final String[] url = URLUtils.extractURLs(body, UPSTREAM_PR, false);
            if (url == null || url.length == 0 || url[0] == null)
                return null;
            else
                return new URL(url[0]);
        } else {
            return null;
        }
    }

    /**
     * Searches PR body for link to upstream issue.
     *
     * @return upstream issue URL or null
     * @throws MalformedURLException if found URL is invalid
     */
    public URL findUpstreamIssueURL() throws MalformedURLException {
        if (isUpstreamRequired()) {
            final String[] url = URLUtils.extractURLs(body, UPSTREAM_ISSUE, false);
            if (url == null || url.length == 0 || url[0] == null)
                return null;
            else
                return new URL(url[0]);
        } else {
            return null;
        }
    }

    /**
     * Searches PR body for link to related issue.
     *
     * @return related issue URL
     * @throws MalformedURLException if found URL is invalid
     */
    public URL findIssueURL() throws MalformedURLException {
        final String[] url = URLUtils.extractURLs(body, ISSUE, false);
        if (url == null || url.length == 0 || url[0] == null)
            return null;
        else
            return new URL(url[0]);
    }

    /**
     * Searches PR body for links of related issues.
     *
     * TODO: Make this return at least all valid URLs, do not fail if one is invalid.
     *
     * @return related issues URLs or empty list
     * @throws MalformedURLException if one of found URLs is invalid
     */
    public List<URL> findRelatedIssuesURL() throws MalformedURLException {
        final String[] urls = URLUtils.extractURLs(body, RELATED_ISSUES, true);
        if (urls == null || urls.length == 0 || urls[0] == null) {
            return Collections.emptyList();
        } else {
            List<URL> issues = new ArrayList<>(urls.length);
            for (String url : urls) {
                issues.add(new URL(url));
            }
            return issues;
        }
    }


    /**
     * Check if this PR has upgrade meta present.
     *
     * @return Contains upgrade meta?
     */
    public boolean hasUpgradeMeta() {
        final Matcher m = UPGRADE.matcher(body);
        return m.find();
    }

    /**
     * TODO: Description - I don't know what this is.
     */
    public PullRequestUpgrade findPullRequestUpgrade() {
        Matcher m = UPGRADE.matcher(body);
        if (!m.find()) {
            return null;
        }
        String upgradeBody = body.substring(m.start(), m.end());
        m = Pattern.compile(UPGRADE_META_BIT_REGEX).matcher(upgradeBody);
        Properties metas = new Properties();
        while (m.find()) {
            final String[] x = upgradeBody.substring(m.start(), m.end()).split("=");
            metas.put(x[0], x[1]);
        }
        return new PullRequestUpgrade(this, metas.getProperty("id"), metas.getProperty("tag"),
                metas.getProperty("version"), metas.getProperty("branch"));
    }

    public boolean isMergeable() {
        return mergeable;
    }

    public boolean isMerged() {
        return merged;
    }

    public MergeableState getMergableState() {
        return mergableState;
    }

    public Date getMergedAt() {
        return mergedAt;
    }

    public boolean isUpgrade() {
        return upgrade;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PullRequest pullRequset = (PullRequest) o;

        return url.equals(pullRequset.url);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PullRequest{" +
                "url=" + url +
                ", state=" + state +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", codebase=" + codebase +
                '}';
    }

}
