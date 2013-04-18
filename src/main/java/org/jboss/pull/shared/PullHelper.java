package org.jboss.pull.shared;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.engineering.bugzilla.Bug;
import com.redhat.engineering.bugzilla.BugzillaClient;
import com.redhat.engineering.bugzilla.BugzillaClientImpl;
import com.redhat.engineering.bugzilla.Flag;

/**
 * A shared functionality regarding mergeable PRs, Github and Bugzilla.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class PullHelper {

    private static final Pattern BUGZILLA_ID_PATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSTREAM_PATTERN = Pattern.compile("github\\.com/jbossas/jboss-as/pull/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/";

    public static final String PM_ACK = "pm_ack";
    public static final String QA_ACK = "qa_ack";
    public static final String DEVEL_ACK = "devel_ack";//FIXME is it dev_ or devel_ack?

    public static final String STATUS_MODIFIED = "MODIFIED";

    private static final Map<String, String> MERGEABLE_FLAGS;
    private static final Map<String, String> UNMERGEABLE_FLAGS;

    static {
        MERGEABLE_FLAGS = new HashMap<String, String>();
        MERGEABLE_FLAGS.put(PM_ACK, "+");
        MERGEABLE_FLAGS.put(QA_ACK, "+");
        UNMERGEABLE_FLAGS = new HashMap<String, String>();
        UNMERGEABLE_FLAGS.put(DEVEL_ACK, "-");
    }

    private String GITHUB_ORGANIZATION;
    private String GITHUB_EAP_REPO;
    private String GITHUB_AS_REPO;
    private String GITHUB_LOGIN;
    private String GITHUB_TOKEN;
    private String GITHUB_BRANCH;

    private static String BUGZILLA_LOGIN;
    private static String BUGZILLA_PASSWORD;

    private IRepositoryIdProvider repositoryEAP;
    private IRepositoryIdProvider repositoryAS;
    private CommitService commitService;
    private IssueService issueService;
    private PullRequestService pullRequestService;

    private BugzillaClient bugzillaClient;

    private Properties props;

    public PullHelper(String configurationFileProperty, String configurationFileDefault) throws Exception {
        try {
            props = Util.loadProperties(configurationFileProperty, configurationFileDefault);

            GITHUB_ORGANIZATION = Util.require(props, "github.organization");
            GITHUB_EAP_REPO = Util.require(props, "github.eap.repo");
            GITHUB_AS_REPO = Util.require(props, "github.as.repo");
            GITHUB_LOGIN = Util.require(props, "github.login");
            GITHUB_TOKEN = Util.get(props, "github.token");
            GITHUB_BRANCH = Util.require(props, "github.branch");

            String flagEapVersion = Util.get(props, "eap.version.flag");
            if (flagEapVersion != null && !flagEapVersion.isEmpty()) {
                MERGEABLE_FLAGS.put(flagEapVersion, "+");
            }

            // initialize client and services
            GitHubClient client = new GitHubClient();
            if (GITHUB_TOKEN != null && GITHUB_TOKEN.length() > 0)
                client.setOAuth2Token(GITHUB_TOKEN);
            repositoryEAP = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_EAP_REPO);
            repositoryAS = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_AS_REPO);
            commitService = new CommitService(client);
            issueService = new IssueService(client);
            pullRequestService = new PullRequestService(client);

            BUGZILLA_LOGIN = Util.require(props, "bugzilla.login");
            BUGZILLA_PASSWORD = Util.require(props, "bugzilla.password");

            // initialize bugzilla client
            bugzillaClient = new BugzillaClientImpl(BUGZILLA_BASE, BUGZILLA_LOGIN, BUGZILLA_PASSWORD);

        } catch (Exception e) {
            System.err.println("Cannot initialize: " + e);
            e.printStackTrace(System.err);
            throw e;
        }
    }


    public boolean isMergeable(PullRequest pull) {
        return isMergeable(pull, null);
    }

    public boolean isMergeable(PullRequest pull, Map<String, String> requiredFlags) {
        boolean mergeable = true;
        mergeable = mergeable && isMergeableByUpstream(pull);
        mergeable = mergeable && isMergeableByBugzilla(pull, requiredFlags);
        return mergeable;
    }

    public boolean isMergeableByUpstream(PullRequest pull) {
        try {
            PullRequest upstreamPull = getUpstreamPullRequest(pull);
            if (upstreamPull != null && upstreamPull.isMerged()) {
                return true;
            }
        } catch (Exception ignore) {
            System.err.printf("Cannot get an upstream pull request of the pull request %d: %s.\n", pull.getNumber(), ignore);
            ignore.printStackTrace(System.err);
        }
        return false;
    }

    public boolean isMergeableByBugzilla(PullRequest pull, Map<String, String> requiredFlags) {
        Bug bug = getBug(pull);
        if (bug == null) {
            return false;
        }

        Map<String, String> flagsToCheck = new HashMap<String, String>(MERGEABLE_FLAGS);
        if (requiredFlags != null) {
            flagsToCheck.putAll(requiredFlags);
        }

        List<Flag> flags = bug.getFlags();
        for (Flag flag : flags) {
            String bannedValue = UNMERGEABLE_FLAGS.get(flag.getName());
            if (bannedValue != null && flag.getStatus().endsWith(bannedValue)) { //FIXME endsWith or equals?
                return false;
            }
            String requiredValue = flagsToCheck.get(flag.getName());
            if (requiredValue != null && flag.getStatus().endsWith(requiredValue)) { //FIXME endsWith or equals?
                flagsToCheck.remove(flag.getName());
            }
        }

        return flagsToCheck.isEmpty();
    }

    public static Integer checkBugzillaId(String body) {
        Matcher matcher = BUGZILLA_ID_PATTERN.matcher(body);
        while (matcher.find()) {    //FIXME what if there are more than one?
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignore) {
                System.err.println("Invalid bug number: " + ignore);
            }
        }
        return null;
    }

    public static Integer checkUpStreamPullRequestId(String body) {
        Matcher matcher = UPSTREAM_PATTERN.matcher(body);
        while (matcher.find()) {    //FIXME what if there are more than one?
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignore) {
                System.err.println("Invalid pull request number: " + ignore);
            }
        }
        return null;
    }

    public Bug getBug(PullRequest pull) {
        Bug bug = null;
        Integer bugId = checkBugzillaId(pull.getBody());
        if (bugId == null) {
            try {
                bug = bugzillaClient.getBug(bugId);
            } catch (Exception ignore) {
                System.err.printf("Cannot get a bug related to the pull request %d: %s.\n", pull.getNumber(), ignore);
            }
        }
        return bug;
    }

    public PullRequest getUpstreamPullRequest(PullRequest pull) throws IOException {
        PullRequest upstreamPull = null;
        Integer pullNo = checkUpStreamPullRequestId(pull.getBody());
        if (pullNo != null) {
            upstreamPull = pullRequestService.getPullRequest(repositoryAS, pullNo);
        }
        return upstreamPull;
    }

    public void updateBugzillaStatus(PullRequest pull, String status) throws Exception {
        Bug bug = getBug(pull);
        if (bug != null) {
            bug.setStatus(status);
            bugzillaClient.update(bug);
        }
    }

    public void postGithubStatus(PullRequest pull, String targetUrl, String status) {
        try {
            CommitStatus commitStatus = new CommitStatus();
            commitStatus.setTargetUrl(targetUrl);
            commitStatus.setState(status);
            commitService.createStatus(repositoryEAP, pull.getHead().getSha(), commitStatus);
        } catch (Exception e) {
            System.err.printf("Problem posting a status build for sha: %s\n", pull.getHead().getSha());
            e.printStackTrace(System.err);
        }
    }

    public void postGithubComment(PullRequest pull, String comment) {
        try {
            issueService.createComment(repositoryEAP, pull.getNumber(), comment);
        } catch (IOException e) {
            System.err.printf("Problem posting a comment build for pull: %d\n", pull.getNumber());
            e.printStackTrace(System.err);
        }
    }


    public Properties getProps() {
        return props;
    }

    public IRepositoryIdProvider getRepositoryEAP() {
        return repositoryEAP;
    }

    public IRepositoryIdProvider getRepositoryAS() {
        return repositoryAS;
    }

    public CommitService getCommitService() {
        return commitService;
    }

    public IssueService getIssueService() {
        return issueService;
    }

    public PullRequestService getPullRequestService() {
        return pullRequestService;
    }

    public String getGithubBranch() {
        return GITHUB_BRANCH;
    }

    public String getGithubLogin() {
        return GITHUB_LOGIN;
    }

}
