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

package org.jboss.set.aphrodite.repository.services.github;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.AbstractRepositoryService;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

/**
 * @author wangc
 *
 */
public abstract class AbstractGithubService extends AbstractRepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.repository.services.github.AbstractGithubService.class);
    private static final int DEFAULT_CACHE_SIZE = 20;

    private static String cacheDir;
    private static  String cacheName;
    private static String cacheSize;
    private static File cacheFile;
    private static Cache cache;
    protected static GitHub github;
    protected static GHUser user;

    public AbstractGithubService(RepositoryType REPOSITORY_TYPE) {
        super(REPOSITORY_TYPE);
    }

    @Override
    public boolean init(RepositoryConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        try {
            if (github != null && github.isCredentialValid()) {
                return true;
            } else {
                return commonGithubInit(config);
            }
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for username: " + config.getUsername(), e);
            return false;
        }
    }

    public static boolean commonGithubInit(RepositoryConfig config) {
        cacheDir = System.getProperty("cacheDir");
        cacheName = System.getProperty("cacheName");

        try {
            if (cacheDir == null || cacheName == null) {
                // no cache specified
                github = GitHub.connect(config.getUsername(), config.getPassword());
            } else {
                // use cache
                cacheFile = new File(cacheDir, cacheName);
                cacheSize = System.getProperty("cacheSize");
                if (cacheSize == null) {
                    cache = new Cache(cacheFile, DEFAULT_CACHE_SIZE * 1024 * 1024); // default 20MB cache
                } else {
                    int size = DEFAULT_CACHE_SIZE;
                    try {
                        size = Integer.valueOf(cacheSize);
                    } catch (NumberFormatException e) {
                        Utils.logWarnMessage(LOG, cacheSize + " is not a valid cache size. Use default size 20MB.");
                    }
                    cache = new Cache(cacheFile, size * 1024 * 1024); // default 20MB cache
                }

                // oauthAccessToken here, if you use text password, call .withPassword()
                github = new GitHubBuilder()
                        .withOAuthToken(config.getPassword(), config.getUsername())
                        .withConnector(new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache))))
                        .build();

            }
            user = github.getUser(config.getUsername());
            return github.isCredentialValid();
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for username: " + config.getUsername(), e);
        }
        return false;
    }

}
