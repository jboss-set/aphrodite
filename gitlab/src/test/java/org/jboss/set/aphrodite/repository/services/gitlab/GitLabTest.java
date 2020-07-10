/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.set.aphrodite.repository.services.gitlab;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.config.StreamConfig;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Test for gitlab that is skipped when username or password are not
 * available.</p>
 *
 * @author rmartinc
 */
public class GitLabTest {

    private static Aphrodite aphrodite;
    private static final String REPO_URL = "https://gitlab.cee.redhat.com";
    private static final String PROJECT_URL = REPO_URL + "/rmartinc/test";
    private static final String PULL_REQUEST_ID = "1";
    private static final String PULL_REQUEST_URL = PROJECT_URL + "/-/merge_requests/" + PULL_REQUEST_ID;
    private static final String PULL_REQUEST_BRANCH = "master";

    // empty username to disable the test (only locally executed)
    private static final String USERNAME = "";
    private static final String PASSWORD = "";

    @BeforeClass
    public static void beforeClass() throws Exception {
        Assume.assumeTrue(!USERNAME.isEmpty() && !PASSWORD.isEmpty());
        // create the aphrodite instance with only the gitlab repository
        RepositoryConfig gitLabConfig = new RepositoryConfig(REPO_URL, USERNAME, PASSWORD, RepositoryType.GITLAB);
        List<RepositoryConfig> repositoryConfigs = new ArrayList<>();
        repositoryConfigs.add(gitLabConfig);
        List<IssueTrackerConfig> issueTrackerConfigs = new ArrayList<>();
        List<StreamConfig> streamConfigs = new ArrayList<>();
        AphroditeConfig config = new AphroditeConfig(issueTrackerConfigs, repositoryConfigs, streamConfigs);
        aphrodite = Aphrodite.instance(config);
    }

    @Test
    public void testGetRepository() throws Exception {
        URL url = new URL(PROJECT_URL);
        Repository repo = aphrodite.getRepository(url);
        Assert.assertEquals(url, repo.getURL());
        Assert.assertTrue(repo.getCodebases().size() >= 1);
        Assert.assertTrue(repo.getCodebases().stream().filter(n -> n.getName().equals(PULL_REQUEST_BRANCH)).findFirst().isPresent());
        Assert.assertTrue(aphrodite.isRepositoryLabelsModifiable(repo));
        List<PullRequest> list = aphrodite.getPullRequestsByState(repo, PullRequestState.OPEN);
        Assert.assertTrue(list.size() > 0);
    }

    @Test
    public void testGetPullRequest() throws Exception {
        URL url = new URL(PULL_REQUEST_URL);
        PullRequest pr = aphrodite.getPullRequest(url);
        Assert.assertNotNull(pr);
        Assert.assertEquals(url, pr.getURL());
        Assert.assertEquals(PULL_REQUEST_ID, pr.getId());
        Assert.assertEquals(PULL_REQUEST_BRANCH, pr.getCodebase().getName());
        Assert.assertFalse(pr.getCommits().isEmpty());
        Assert.assertTrue(pr.findReferencedPullRequests().size() >= 1);
    }

    private Label getMissingLabel(List<Label> repoLabels, List<Label> prLabels) {
        for (Label repoLabel : repoLabels) {
            boolean found = false;
            for (Label prLabel : prLabels) {
                if (prLabel.getName().equals(repoLabel.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return repoLabel;
            }
        }
        return null;
    }

    @Test
    public void testLabels() throws Exception {
        URL url = new URL(PULL_REQUEST_URL);
        PullRequest pr = aphrodite.getPullRequest(url);
        Assert.assertNotNull(pr);
        Repository repo = aphrodite.getRepository(url);
        Assert.assertNotNull(repo);

        List<Label> repoLabels = aphrodite.getLabelsFromRepository(repo);
        Assert.assertTrue(repoLabels.size() > 0);
        List<Label> prLabels = pr.getLabels();
        Label toAdd = getMissingLabel(repoLabels, prLabels);

        pr.addLabel(toAdd);
        List<Label> newPrLabels = pr.getLabels();
        Assert.assertEquals(prLabels.size() + 1, newPrLabels.size());
        Assert.assertTrue(newPrLabels.stream().filter(l -> l.getName().equals(toAdd.getName())).findFirst().isPresent());

        pr.removeLabel(toAdd);
        newPrLabels = pr.getLabels();
        Assert.assertEquals(prLabels.size(), newPrLabels.size());
        Assert.assertTrue(!newPrLabels.stream().filter(l -> l.getName().equals(toAdd.getName())).findFirst().isPresent());

        pr.setLabels(Collections.singletonList(toAdd));
        newPrLabels = pr.getLabels();
        Assert.assertEquals(1, newPrLabels.size());
        Assert.assertTrue(newPrLabels.iterator().next().getName().equals(toAdd.getName()));

        pr.setLabels(prLabels);
        newPrLabels = pr.getLabels();
        Assert.assertEquals(prLabels.size(), newPrLabels.size());
    }

    @Test
    public void testComment() throws Exception {
        URL url = new URL(PULL_REQUEST_URL);
        PullRequest pr = aphrodite.getPullRequest(url);
        Assert.assertNotNull(pr);
        pr.addComment("comment added by the test-suite");
    }
}
