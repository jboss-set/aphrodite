/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.pull.shared;

import java.util.regex.Pattern;

/**
 * @author Chao Wang chaowan@redhat.com
 *
 */
public class Constants {

    // Regexp patterns
    public static final Pattern BUILD_OUTCOME = Pattern.compile("outcome was (\\*\\*)?+(SUCCESS|FAILURE|ABORTED)(\\*\\*)?+ using a merge of ([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern PENDING = Pattern.compile(".*Build.*merging.*has\\W+been\\W+triggered.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern RUNNING = Pattern.compile(".*Build.*merging.*has\\W+been\\W+started.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern FINISHED = Pattern.compile(".*Build.*merging.*has\\W+been\\W+finished.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern MERGE = Pattern.compile(".*(re)?merge\\W+this\\W+please.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static final Pattern FORCE_MERGE = Pattern.compile(".*force\\W+merge\\W+this.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static final Pattern UPSTREAM_NOT_REQUIRED = Pattern.compile(".*no.*upstream.*required.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern BUGZILLA_ID_PATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern RELATED_JIRA_PATTERN = Pattern.compile(".*issues\\.jboss\\.org/browse/([a-zA-Z_0-9-]*)", Pattern.CASE_INSENSITIVE);

    // This has to match two patterns
    // * https://github.com/uselessorg/jboss-eap/pull/4
    // * https://api.github.com/repos/uselessorg/jboss-eap/pulls/4
    public static final Pattern RELATED_PR_PATTERN = Pattern.compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);

    // URL bases
    public static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/";
    public static final String BUGZILLA_BASE_ID = "https://bugzilla.redhat.com/show_bug.cgi?id=";
    public static final String BUGZILLA_HOST = "bugzilla.redhat.com";

    public static final String JIRA_BASE = "https://issues.jboss.org";
    public static final String JIRA_BASE_BROWSE = "https://issues.jboss.org/browse/";
    public static final String JIRA_HOST = "issues.jboss.org";

}
