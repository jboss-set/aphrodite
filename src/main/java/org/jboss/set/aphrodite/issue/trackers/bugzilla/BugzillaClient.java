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

package org.jboss.set.aphrodite.issue.trackers.bugzilla;

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
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.aphrodite.domain.SearchCriteria;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jboss.set.aphrodite.issue.trackers.bugzilla.BugzillaFields.*;

/**
 * @author Ryan Emerson
 */
public class BugzillaClient {

    private static final Log LOG = LogFactory.getLog(BugzillaClient.class);
    public static final Pattern ID_PARAM_PATTERN = Pattern.compile("id=([^&]+)");

    private final IssueWrapper WRAPPER = new IssueWrapper();
    private final URL baseURL;
    private final Map<String, Object> loginDetails;

    public BugzillaClient(URL baseURL, String login, String password) throws IllegalStateException {
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

    public Issue getIssue(URL url) throws NotFoundException {
        String trackerId = Utils.getTrackerIdFromUrl(ID_PARAM_PATTERN, url);
        return getIssue(trackerId);
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
            try {
                return WRAPPER.bugzillaBugToIssue(results, baseURL);
            } catch (MalformedURLException e) {
                Utils.logException(LOG, "Unable to create Issue Object.", e);
            }
        } else {
            Utils.logWarnMessage(LOG, "Zero or more than one bug found with id: " + trackerId);
        }
        throw new NotFoundException();
    }

    public Issue getIssueWithComments(URL url) throws NotFoundException {
        String trackerId = Utils.getTrackerIdFromUrl(ID_PARAM_PATTERN, url);
        return getIssueWithComments(trackerId);
    }

    public Issue getIssueWithComments(String trackerId) throws NotFoundException {
        Issue issue = getIssue(trackerId);
        setCommentsForIssue(issue);
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

        return getCommentsForIssue(Utils.getTrackerIdFromUrl(ID_PARAM_PATTERN, issue.getURL()));
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

    public List<Issue> searchIssues(SearchCriteria criteria) {
        Map<String, Object> queryMap = new BugzillaQueryBuilder(criteria, loginDetails).getQueryMap();

        List<Issue> issues = new ArrayList<>();
        Map<String, ?> resultMap = executeRequest(XMLRPC.RPC_STRUCT, METHOD_SEARCH, queryMap);
        if (resultMap != null && !resultMap.isEmpty()) {
            final Object[] bugs = XMLRPC.cast(XMLRPC.RPC_ARRAY, resultMap.get(RESULT_BUGS));
            for (Map<String, Object> struct : XMLRPC.iterable(XMLRPC.RPC_STRUCT, bugs)) {
                try {
                    Issue issue = WRAPPER.bugzillaBugToIssue(struct, baseURL);
                    setCommentsForIssue(issue);
                    issues.add(issue);
                } catch (MalformedURLException e) {
                    Utils.logException(LOG, "Unable to create Issue Object.", e);
                }
            }
        }
        return issues;
    }

    public boolean updateIssue(Issue issue) {
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
        String trackerId = issue.getTrackerId().orElse(Utils.getTrackerIdFromUrl(ID_PARAM_PATTERN, issue.getURL()));
        return postComment(new Integer(trackerId), comment.getBody(), comment.isPrivate());
    }

    public boolean postComment(int id, String comment, boolean isPrivate) {
        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ID, id);
        params.put(COMMENT, comment);
        params.put(PRIVATE_COMMENT, isPrivate);
        return runCommand(METHOD_ADD_COMMENT, params);
    }

    public boolean updateFlags(int ids, String name, FlagStatus status) {
        String flagStatus = status.getSymbol();
        Map<String, String> updates = new HashMap<>();
        updates.put(NAME, name);
        updates.put(STATUS, flagStatus);
        Object[] updateArray = {updates};

        Map<String, Object> params = new HashMap<>(loginDetails);
        params.put(ISSUE_IDS, ids);
        params.put(UPDATE_FIELDS, updateArray);
        params.put(RESULT_PERMISSIVE_SEARCH, true);

        return runCommand(METHOD_UPDATE_BUG, params);
    }

    @SuppressWarnings("unchecked")
    private List<Comment> getCommentList(Map<String, Object> issue) {
        List<Comment> issueComments = new ArrayList<>();
        // Get all comments associated with issue
        for (Object[] comments : XMLRPC.iterable(XMLRPC.RPC_ARRAY, issue.values())) {
            // Iterate each comment and retrieve relevant fields
            for (Map<String, Object> comment : XMLRPC.iterable(XMLRPC.RPC_STRUCT, comments)) {
                String id = String.valueOf(comment.get(COMMENT_ID));
                String body = (String) comment.get(COMMENT_BODY);
                boolean isPrivate = (Boolean) comment.get(COMMENT_IS_PRIVATE);
                issueComments.add(new Comment(id, body, isPrivate));
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

        @SuppressWarnings("unchecked")
        XMLRPC(final Class<?> cls) {
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
