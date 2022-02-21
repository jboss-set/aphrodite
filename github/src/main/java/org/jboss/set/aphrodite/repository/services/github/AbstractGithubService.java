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
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * @author wangc
 *
 */
public abstract class AbstractGithubService extends AbstractRepositoryService {

    private static final Log LOG = LogFactory.getLog(org.jboss.set.aphrodite.repository.services.github.AbstractGithubService.class);
    private static final String CACHE_DIR = "cacheDir";
    private static final String CACHE_NAME = "cacheName";
    private static final String CACHE_SIZE = "cacheSize";
    private static final int DEFAULT_CACHE_SIZE = 20;

    private static String cacheDir;
    private static  String cacheName;
    private static String cacheSize;
    private static File cacheFile;
    private static Cache cache;
    protected static GitHub github;
    protected static GHUser user;

    public AbstractGithubService(RepositoryType repositoryType) {
        super(repositoryType);
    }

    @Override
    public boolean init(RepositoryConfig config) {
        boolean parentInitiated = super.init(config);
        if (!parentInitiated)
            return false;

        if (github != null && github.isCredentialValid()) {
            return true;
        } else {
            return commonGithubInit(config);
        }
    }

    public static boolean commonGithubInit(RepositoryConfig config) {
        cacheDir = getValueFromPropertyAndEnv(CACHE_DIR);
        cacheName = getValueFromPropertyAndEnv(CACHE_NAME);

        try {
            if (cacheDir == null || cacheName == null) {
                // no cache specified
                github = GitHub.connect(config.getUsername(), config.getPassword());
            } else {
                // use cache
                cacheFile = new File(cacheDir, cacheName);
                cacheSize = getValueFromPropertyAndEnv(CACHE_SIZE);
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
                        .withConnector(new OkHttpConnector(new OkHttpClient.Builder().cache(cache).build()))
                        .build();

            }
            user = github.getUser(config.getUsername());
            return github.isCredentialValid();
        } catch (IOException e) {
            Utils.logException(LOG, "Authentication failed for username: " + config.getUsername(), e);
        }
        return false;
    }

    private static String getValueFromPropertyAndEnv(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            return System.getenv(key);
        }
        return value;
    }

}
