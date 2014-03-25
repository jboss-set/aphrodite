package org.jboss.shared.connectors;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.connectors.RedhatPullRequest;
import org.jboss.shared.connectors.bugzilla.MockBZHelper;
import org.jboss.shared.connectors.github.MockGithubHelper;

@Test
public class TestRedhatPullRequest {

    @Test
    public void testFindBZ() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing BZ macthing.\n BZ: https://bugzilla.redhat.com/show_bug.cgi?id=953471");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.isBZInDescription());
    }

    @Test
    public void testNoBZ() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing BZ matching.\n");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertFalse(pullRequest.isBZInDescription());
    }

    @Test
    public void testFindJIRA() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing JIRA matching. JIRA: https://issues.jboss.org/browse/EAP6-77");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.isJiraInDescription());
    }

    @Test
    public void testNoJIRA() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing JIRA matching.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertFalse(pullRequest.isJiraInDescription());
    }

    @Test
    public void testFindUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream: https://github.com/uselessorg/jboss-eap/pull/2");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertFalse(pullRequest.getRelatedPullRequests().isEmpty());
    }

    @Test
    public void testNoUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.getRelatedPullRequests().isEmpty());
    }

    @Test
    public void testNotRequiredUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.isUpstreamRequired());
    }

    @Test
    public void testMilestoneNull() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.isGithubMilestoneNullOrDefault());
    }

    @Test
    public void testMilestoneDefault() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");
        pr.setMilestone(new Milestone().setTitle("6.x"));

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertTrue(pullRequest.isGithubMilestoneNullOrDefault());
    }

    @Test
    public void testMilestoneNotNullOrDefault() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");
        pr.setMilestone(new Milestone().setTitle("6.2.2"));

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, new MockBZHelper(), new MockGithubHelper());

        AssertJUnit.assertFalse(pullRequest.isGithubMilestoneNullOrDefault());
    }
}
