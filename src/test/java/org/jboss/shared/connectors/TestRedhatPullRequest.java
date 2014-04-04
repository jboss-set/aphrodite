package org.jboss.shared.connectors;

import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.connectors.IssueHelper;
import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.github.GithubHelper;
import org.jboss.pull.shared.connectors.jira.JiraHelper;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.connectors.RedhatPullRequest;

import java.net.URL;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@Test
public class TestRedhatPullRequest {

    private static final String BZ_953471 = "953471";
    private static final String EAP6_77 = "EAP6-77";
    private static final String GH_ORG = "uselessorg";
    private static final String GH_PROJECT = "jboss-eap";
    private static final int GH_PULL_NUMBER = 2;
    private IssueHelper bzHelper = null;
    private IssueHelper jiraHelper = null;
    private GithubHelper githubHelper = null;

    @BeforeTest
    public void setUpMocks() throws Exception {
        // Mocking the helpers, etc.
        this.bzHelper = mock(BZHelper.class);
        URL bzURL = new URL(Constants.BUGZILLA_BASE_ID + BZ_953471);
        when(bzHelper.accepts(bzURL)).thenReturn(true);

        this.jiraHelper = mock(JiraHelper.class);
        URL jiraURL = new URL(Constants.JIRA_BASE_BROWSE + EAP6_77);
        when(jiraHelper.accepts(jiraURL)).thenReturn(true);

        // Now for the GH helper
        this.githubHelper = mock(GithubHelper.class);
        PullRequest testPull = new PullRequest();
        testPull.setBody("Testing Upstream matching. Upstream: https://github.com/uselessorg/jboss-eap/pull/2");
        when(githubHelper.getPullRequest(GH_ORG,  GH_PROJECT, GH_PULL_NUMBER)).thenReturn(testPull);
    }

    @AfterTest
    public void killMocks() {
        this.bzHelper = null;
        this.jiraHelper = null;
        this.githubHelper = null;
    }

    @Test
    public void testFindBZ() {
        PullRequest pr = new PullRequest();
        String bodyURL = Constants.BUGZILLA_BASE_ID + BZ_953471;
        pr.setBody("Testing BZ matching.\n BZ: " + bodyURL);

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.hasBZInDescription());
    }

    @Test
    public void testNoBZ() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing BZ matching.\n");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertFalse(pullRequest.hasBZInDescription());
    }

    @Test
    public void testFindJIRA() {
        PullRequest pr = new PullRequest();
        String bodyURL = Constants.JIRA_BASE_BROWSE + EAP6_77;
        pr.setBody("Testing JIRA matching. JIRA: " + bodyURL);

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.hasJiraInDescription());
    }

    @Test
    public void testNoJIRA() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing JIRA matching.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertFalse(pullRequest.hasJiraInDescription());
    }

    @Test
    public void testFindUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream: https://github.com/uselessorg/jboss-eap/pull/2");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertFalse(pullRequest.getRelatedPullRequests().isEmpty());
    }

    @Test
    public void testNoUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.getRelatedPullRequests().isEmpty());
    }

    @Test
    public void testNotRequiredUpstream() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.isUpstreamRequired());
    }

    @Test
    public void testMilestoneNull() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.isGithubMilestoneNullOrDefault());
    }

    @Test
    public void testMilestoneDefault() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");
        pr.setMilestone(new Milestone().setTitle("6.x"));

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertTrue(pullRequest.isGithubMilestoneNullOrDefault());
    }

    @Test
    public void testMilestoneNotNullOrDefault() {
        PullRequest pr = new PullRequest();
        pr.setBody("Testing Upstream matching. Upstream not required.");
        pr.setMilestone(new Milestone().setTitle("6.2.2"));

        RedhatPullRequest pullRequest = new RedhatPullRequest(pr, bzHelper, jiraHelper, githubHelper);

        assertFalse(pullRequest.isGithubMilestoneNullOrDefault());
    }
}
