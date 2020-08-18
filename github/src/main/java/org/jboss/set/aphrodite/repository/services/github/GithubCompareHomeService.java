/*
 * Copyright 2020 Red Hat, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.domain.Compare;
import org.jboss.set.aphrodite.domain.spi.CompareHome;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;

import static org.jboss.set.aphrodite.repository.services.common.RepositoryUtils.createRepositoryIdFromUrl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GithubCompareHomeService extends AbstractGithubService implements CompareHome {
    private static final Log LOG = LogFactory.getLog(GithubCompareHomeService.class);
    private static final GitHubWrapper WRAPPER = new GitHubWrapper();

    public GithubCompareHomeService(Aphrodite aphrodite) {
        super(RepositoryType.GITHUB);
        AphroditeConfig configuration = aphrodite.getConfig();
        this.init(configuration);
    }

    @Override
    public Compare getCompare(URL url, String tag1, String tag2) {
        try {
            return WRAPPER.toCompare(github.getRepository(createRepositoryIdFromUrl(url)).getCompare(tag1,tag2));
        } catch (IOException e) {
            Utils.logWarnMessage(LOG, "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return new Compare();
        }
    }

    @Override
    public List<String> getTags(URL url) {
        try {
            GHRepository repo = github.getRepository(createRepositoryIdFromUrl(url));
            return repo.listTags().toList().stream().map(GHTag::getName).collect(Collectors.toList());
        } catch (IOException e) {
            Utils.logWarnMessage(LOG, "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public List<String> getBranches(URL url) {
        try {
            GHRepository repo = github.getRepository(createRepositoryIdFromUrl(url));
            return new ArrayList<>(repo.getBranches().keySet());
        } catch (IOException e) {
            Utils.logWarnMessage(LOG, "repository : " + url + " is not accessable due to " + e.getMessage() + ". Check repository link and your account permission.");
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    protected Log getLog() {
        return LOG;
    }
}
