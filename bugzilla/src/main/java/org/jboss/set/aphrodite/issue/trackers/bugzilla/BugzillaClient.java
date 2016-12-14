/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.API_URL;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMMENT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMMENT_BODY;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMMENT_FIELDS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMMENT_ID;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.COMMENT_IS_PRIVATE;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ESTIMATED_TIME;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.FILTER_SHARER_ID;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ID;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.ISSUE_IDS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.LOGIN;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_ADD_COMMENT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_FILTER_SEARCH;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_GET_BUG;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_GET_COMMENT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_SEARCH;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_UPDATE_BUG;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.METHOD_USER_LOGIN;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.NAME;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.PASSWORD;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.PRIVATE_COMMENT;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.RESULT_BUGS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.RESULT_FIELDS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.RESULT_INCLUDE_FIELDS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.RESULT_PERMISSIVE_SEARCH;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.STATUS;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.TARGET_RELEASE;
import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.UPDATE_FIELDS;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Comment;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author Ryan Emerson
 */
public class BugzillaClient {

    private static final Log LOG = LogFactory.getLog(BugzillaClient.class);

    static final Pattern ID_PARAM_PATTERN = Pattern.compile("id=([^&]+)");
    static final Pattern FILTER_NAME_PARAM_PATTERN = Pattern.compile("namedcmd=([^&]+)");
    static final Pattern SHARER_ID_PARAM_PATTERN = Pattern.compile("sharer_id=([^&]+)");

    private static final Pattern RELATED_PR_PATTERN = Pattern
            .compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);

    private final ExecutorService executorService;
    private final IssueWrapper WRAPPER = new IssueWrapper();
    private final URL baseURL;
    private final Map<String, Object> loginDetails;

    public BugzillaClient(URL baseURL, String login, String password, ExecutorService executorService) throws IllegalStateException {
        this.executorService = executorService;
        this.baseURL = baseURL;

        Map<String, String> params = new HashMap<>();
        if (login != null)
            params.put(LOGIN, login);
        if (password != null)
            params.put(PASSWORD, password);
        loginDetails = Collections.unmodifiableMap(params);

        // Check that the provided login details are correct - Fail fast.
        runCommand(METHOD_USER_LOGIN, params);
    }

    public Issue getIssue(String trackerId) throws NotFoundException {
        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(RESULT_INCLUDE_FIELDS, RESULT_FIELDS);
        params.put(ISSUE_IDS, trackerId);
        params.put(RESULT_PERMISSIVE_SEARCH, true);

        Map<String, ?> resultMap = executeRequest(XMLRPC.RPC_STRUCT, METHOD_GET_BUG, params);
        Object[] bugs = (Object[]) resultMap.get(RESULT_BUGS);
        if (bugs.length == 1) {
            @SuppressWarnings("unchecked")
            Map<String, Object> results = (Map<String, Object>) bugs[0];
            return WRAPPER.bugzillaBugToIssue(results, baseURL);
        } else {
            Utils.logWarnMessage(LOG, "Zero or more than one bug found with id: " + trackerId);
        }
        throw new NotFoundException("No issues found with id: " + trackerId);
    }

    public List<Issue> getIssues(Collection<URL> urls) {
        List<String> ids = new ArrayList<>();
        for (URL url : urls) {
            try {
                ids.add(Utils.getParamaterFromUrl(ID_PARAM_PATTERN, url));
            } catch (NotFoundException e) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Unable to extract trackerId from: " + url);
            }
        }

        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(RESULT_INCLUDE_FIELDS, RESULT_FIELDS);
        params.put(ISSUE_IDS, ids.toArray());
        params.put(RESULT_PERMISSIVE_SEARCH, true);

        Map<String, ?> resultMap = executeRequest(XMLRPC.RPC_STRUCT, METHOD_GET_BUG, params);
        Object[] bugs = (Object[]) resultMap.get(RESULT_BUGS);

        List<Issue> issues = new ArrayList<>();
        for (Object bugObject : bugs) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bug = (Map<String, Object>) bugObject;
            issues.add(WRAPPER.bugzillaBugToIssue(bug, baseURL));
        }
        return issues;
    }

    public Issue getIssueWithComments(URL url) throws NotFoundException {
        String trackerId = Utils.getParamaterFromUrl(ID_PARAM_PATTERN, url);
        return getIssueWithComments(trackerId);
    }

    public Issue getIssueWithComments(String trackerId) throws NotFoundException {
        Issue issue = getIssue(trackerId);
        setCommentsForIssue(issue);
        setPullRequestsForIssue(issue); // order matters, depends on comments
        return issue;
    }

    private void setCommentsForIssue(Issue issue) {
        try {
            issue.setComments(getCommentsForIssue(issue));
        } catch (NotFoundException e) {
            Utils.logException(LOG, "Unable to retrieve comments for issue: ", e);
        }
    }

    public List<Comment> getCommentsForIssue(Issue issue) throws NotFoundException {
        if (issue == null)
            throw new IllegalArgumentException("The provided issue cannot be null.");

        if (issue.getTrackerId().isPresent())
            return getCommentsForIssue(issue.getTrackerId().get());

        return getCommentsForIssue(Utils.getParamaterFromUrl(ID_PARAM_PATTERN, issue.getURL()));
    }

    public Map<String, List<Comment>> getCommentsForIssues(Map<String, Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            Collections.emptyMap();
        }

        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ISSUE_IDS, extractIssueIdsList(issues.values()));
        params.put(RESULT_INCLUDE_FIELDS, COMMENT_FIELDS);

        return buildMapOfCommentsIndexedByBugId(executeRequest(XMLRPC.RPC_STRUCT, METHOD_GET_COMMENT, params));
    }

    private Map<String, List<Comment>> buildMapOfCommentsIndexedByBugId(Map<String, Object> results) {
        Map<String, List<Comment>> commentsMap = new HashMap<>();
        if (results != null && !results.isEmpty() && results.containsKey(RESULT_BUGS)) {
            for (Map<String, Object> comments : XMLRPC.iterable(XMLRPC.RPC_STRUCT, results.values())) {
                for (Entry<String, Object> comment : comments.entrySet()) {
                    final String bugId = comment.getKey();
                    commentsMap.put(
                            bugId,
                            buildCommentsForBug(
                                    XMLRPC.cast(XMLRPC.RPC_ARRAY,
                                            XMLRPC.cast(XMLRPC.RPC_STRUCT, comment.getValue()).get("comments"))));
                }
            }
        }
        return commentsMap;
    }

    private List<Comment> buildCommentsForBug(final Object[] commentObjArray) {
        List<Comment> comments = new ArrayList<>(commentObjArray.length);
        for (Object o : commentObjArray) {
            comments.add(buildComment(XMLRPC.cast(XMLRPC.RPC_STRUCT, o)));
        }
        return comments;
    }

    private Comment buildComment(Map<String, Object> comment) {
        String id = String.valueOf(comment.get(COMMENT_ID));
        String body = (String) comment.get(COMMENT_BODY);
        boolean isPrivate = (Boolean) comment.get(COMMENT_IS_PRIVATE);
        return new Comment(id, body, isPrivate);
    }

    private void setPullRequestsForIssue(Issue issue) {
        Set<URL> pullRequests = new HashSet<>();
        issue.getComments().stream().forEach(e -> extractPullRequests(pullRequests, e.getBody()));
        issue.setPullRequests(pullRequests);
    }

    private void extractPullRequests(Set<URL> pullRequests, String messageBody) {
        Matcher matcher = RELATED_PR_PATTERN.matcher(messageBody);
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                String urlStr = "https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pull/" + matcher.group(3);
                try {
                    URL url = new URL(urlStr);
                    pullRequests.add(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL:" + urlStr, e);
                }
            }
        }
    }

    private Object[] extractIssueIdsList(Collection<Issue> collection) {
        return collection.stream()
                .filter(Objects::nonNull)
                .map(issue -> issue.getTrackerId().get())
                .collect(Collectors.toList())
                .toArray();
    }

    public List<Comment> getCommentsForIssue(String trackerId) {
        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ISSUE_IDS, trackerId);
        params.put(RESULT_INCLUDE_FIELDS, COMMENT_FIELDS);
        Map<String, ?> results = executeRequest(XMLRPC.RPC_STRUCT, METHOD_GET_COMMENT, params);

        if (results != null && !results.isEmpty() && results.containsKey(RESULT_BUGS)) {
            Map<String, Object> issues = XMLRPC.cast(XMLRPC.RPC_STRUCT, results.get(RESULT_BUGS));
            return getCommentList(XMLRPC.cast(XMLRPC.RPC_STRUCT, issues.get(trackerId)));
        }
        return new ArrayList<>();
    }

    public List<Issue> searchIssuesByFilter(URL filterUrl) throws NotFoundException {
        String filterName = Utils.getParamaterFromUrl(FILTER_NAME_PARAM_PATTERN, filterUrl);
        int sharerId = Integer.parseInt(Utils.getParamaterFromUrl(SHARER_ID_PARAM_PATTERN, filterUrl));
        Map<String, Object> queryMap = new HashMap<>(loginDetails);
        queryMap.put(METHOD_FILTER_SEARCH, filterName);
        queryMap.put(FILTER_SHARER_ID, sharerId);
        queryMap.put(RESULT_INCLUDE_FIELDS, RESULT_FIELDS);

        try {
            return searchIssues(queryMap);
        } catch (RuntimeException e) {
            throw new NotFoundException("Unable to retrieve issues associated with filter url: " + filterUrl, e);
        }
    }

    public List<Issue> searchIssues(SearchCriteria criteria) {
        return searchIssues(criteria, -1);
    }

    public List<Issue> searchIssues(SearchCriteria criteria, int defaultIssueLimit) {
        Map<String, Object> queryMap = new BugzillaQueryBuilder(criteria, loginDetails, defaultIssueLimit).getQueryMap();
        if (queryMap == null)
            return new ArrayList<>();
        return searchIssues(queryMap);
    }

    private List<Issue> searchIssues(Map<String, Object> queryMap) {
        List<Issue> issueList = new ArrayList<>(0);
        Map<String, ?> resultMap = executeRequest(XMLRPC.RPC_STRUCT, METHOD_SEARCH, queryMap);
        if (resultMap != null && !resultMap.isEmpty()) {
            Map<String, Issue> issues = fetchAllIssues(XMLRPC.cast(XMLRPC.RPC_ARRAY, resultMap.get(RESULT_BUGS)));
            Map<String, List<Comment>> comments = getCommentsForIssues(issues);

            issueList = issues.keySet().stream()
                    .filter(Objects::nonNull)
                    .map(id -> associateCommentsToIssue(issues.get(id), comments))
                    .collect(Collectors.toList());
        }
        return issueList;
    }

    private Issue associateCommentsToIssue(Issue issue, Map<String, List<Comment>> comments) {
        issue.setComments(comments.get(issue.getTrackerId().get()));
        return issue;
    }

    private Map<String, Issue> fetchAllIssues(final Object[] bugs) {
        Map<String, Issue> issues = new HashMap<>();
        for (Map<String, Object> struct : XMLRPC.iterable(XMLRPC.RPC_STRUCT, bugs)) {
            Issue issue = WRAPPER.bugzillaBugToIssue(struct, baseURL);
            issues.put(issue.getTrackerId().get(), issue);
        }
        return issues;
    }

    public boolean updateIssue(Issue issue) throws AphroditeException {
        Map<String, Object> params = WRAPPER.issueToBugzillaBug(issue, loginDetails);
        return runCommand(METHOD_UPDATE_BUG, params);
    }

    public boolean updateTargetRelease(int id, final String... targetRelease) {
        return updateField(id, TARGET_RELEASE, targetRelease);
    }

    public boolean updateStatus(int id, IssueStatus status) {
        return updateField(id, STATUS, status);
    }

    public boolean updateTargetMilestone(int id, String targetMilestone) {
        return updateField(id, TARGET_RELEASE, targetMilestone);
    }

    public boolean updateEstimate(int id, double worktime) {
        return updateField(id, ESTIMATED_TIME, worktime);
    }

    public boolean postComment(Issue issue, Comment comment) throws NotFoundException {
        String trackerId = issue.getTrackerId().orElse(Utils.getParamaterFromUrl(ID_PARAM_PATTERN, issue.getURL()));
        return postComment(new Integer(trackerId), comment.getBody(), comment.isPrivate());
    }

    public boolean postComment(int id, String comment, boolean isPrivate) {
        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ID, id);
        params.put(COMMENT, comment);
        params.put(PRIVATE_COMMENT, isPrivate);
        return runCommand(METHOD_ADD_COMMENT, params);
    }

    public boolean postComment(Map<Issue, Comment> commentMap) {
        List<CompletableFuture<Boolean>> requests = commentMap.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(
                        () -> postCommentAndLogExceptions(entry.getKey(), entry.getValue()), executorService))
                .collect(Collectors.toList());

        return requests.stream()
                .map(CompletableFuture::join)
                .noneMatch(failed -> !failed);
    }

    public boolean postComment(Collection<Issue> issues, Comment comment) {
        List<CompletableFuture<Boolean>> requests = issues.stream()
                .map(issue -> CompletableFuture.supplyAsync(
                        () -> postCommentAndLogExceptions(issue, comment), executorService))
                .collect(Collectors.toList());

        return requests.stream()
                .map(CompletableFuture::join)
                .noneMatch(failed -> !failed);
    }

    private boolean postCommentAndLogExceptions(Issue issue, Comment comment) {
        try {
            return postComment(issue, comment);
        } catch (NotFoundException e) {
            Utils.logException(LOG, e);
            return false;
        }
    }

    public boolean updateFlags(int ids, String name, FlagStatus status) {
        String flagStatus = status.getSymbol();
        Map<String, String> updates = new HashMap<>();
        updates.put(NAME, name);
        updates.put(STATUS, flagStatus);
        Object[] updateArray = { updates };

        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ISSUE_IDS, ids);
        params.put(UPDATE_FIELDS, updateArray);
        params.put(RESULT_PERMISSIVE_SEARCH, true);

        return runCommand(METHOD_UPDATE_BUG, params);
    }

    private List<Comment> getCommentList(Map<String, Object> issues) {
        List<Comment> issueComments = new ArrayList<>();
        for (Object[] comments : XMLRPC.iterable(XMLRPC.RPC_ARRAY, issues.values())) {
            for (Map<String, Object> comment : XMLRPC.iterable(XMLRPC.RPC_STRUCT, comments)) {
                issueComments.add(buildComment(comment));
            }
        }
        return issueComments;
    }

    private boolean updateField(int bugzillaId, String field, Object content) {
        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ID, bugzillaId);
        params.put(field, content);
        return runCommand(METHOD_UPDATE_BUG, params);
    }

    private <T> T executeRequest(final XMLRPC<T> type, String method, Object... params) {
        try {
            return type.cast(getRpcClient().execute(method, params));
        } catch (XmlRpcException e) {
            Utils.logException(LOG, e);
            throw new RuntimeException(e); // TODO improve exception handling
        }
    }

    private XmlRpcClient getRpcClient() {
        String apiURL = baseURL + API_URL;
        XmlRpcClient rpcClient;
        rpcClient = new XmlRpcClient();

        try {
            URL url = new URL(apiURL);
            rpcClient.setConfig(getClientConfig(url));
        } catch (MalformedURLException e) {
            Utils.logException(LOG, e);
            throw new RuntimeException(e);
        }
        return rpcClient;
    }

    private XmlRpcClientConfig getClientConfig(URL apiURL) {
        XmlRpcClientConfigImpl config;
        config = new XmlRpcClientConfigImpl();
        config.setServerURL(apiURL);
        return config;
    }

    private boolean runCommand(String method, Object... params) {
        try {
            getRpcClient().execute(method, params);
            return true;
        } catch (XmlRpcException e) {
            throw new IllegalStateException(e);
        }
    }

    // TODO is there a cleaner way to do this?
    private static class XMLRPC<T> {
        static final XMLRPC<Object[]> RPC_ARRAY = new XMLRPC<>(Object[].class);
        static final XMLRPC<Map<String, Object>> RPC_STRUCT = new XMLRPC<>(Map.class);

        final Class<T> cls;

        @SuppressWarnings("unchecked") XMLRPC(final Class<?> cls) {
            this.cls = (Class<T>) cls;
        }

        T cast(final Object obj) {
            return cls.cast(obj);
        }

        static <T> T cast(final XMLRPC<T> type, Object obj) {
            return type.cast(obj);
        }

        static <T> Iterable<T> iterable(final XMLRPC<T> type, final Collection<Object> c) {
            final Iterator<Object> it = c.iterator();
            return () -> new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return type.cast(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        static <T> Iterable<T> iterable(final XMLRPC<T> type, final Object[] array) {
            final Iterator<Object> it = Arrays.asList(array).iterator();
            return () -> new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return type.cast(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }
    }
}
