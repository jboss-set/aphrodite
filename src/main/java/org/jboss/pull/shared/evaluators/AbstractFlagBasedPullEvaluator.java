/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.pull.shared.evaluators;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.Bug;
import org.jboss.pull.shared.Flag;
import org.jboss.pull.shared.PullHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * An abstract evaluator based on Bugzilla flags resolution.
 * It can be configured to which flags are needed in order
 * to merge a pull request.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractFlagBasedPullEvaluator extends BasePullEvaluator {

    public static final String PM_ACK = "pm_ack";
    public static final String QA_ACK = "qa_ack";
    public static final String DEVEL_ACK = "devel_ack";

    private static final Set<String> REQUIRED_FLAGS;

    static {
        REQUIRED_FLAGS = new HashSet<String>();
        REQUIRED_FLAGS.add(PM_ACK);
        REQUIRED_FLAGS.add(DEVEL_ACK);
        REQUIRED_FLAGS.add(QA_ACK);
    }

    /**
     * Define additional flags needed for a particular EAP version.
     * @return
     */
    protected abstract String[] additionalRequiredFlags(final Properties properties);


    @Override
    public void init(final PullHelper helper, final Properties configuration) {
        super.init(helper, configuration);
        additionalRequiredFlags(configuration);
    }

    @Override
    public Result isMergeable(final PullRequest pull) {
        return isMergeable(pull, null);
    }

    // it looks like nobody is using the second param atm   FIXME
    protected Result isMergeable(final PullRequest pull, final Set<String> requiredFlags) {
        final Result mergeable;
        mergeable = isMergeableByUpstream(pull);
        mergeable.and(isMergeableByBugzilla(pull, requiredFlags));
        return mergeable;
    }

    protected Result isMergeableByUpstream(final PullRequest pull) {
        final Result mergeable = new Result(true);

        try {
            final List<PullRequest> upstreamPulls = helper.getUpstreamPullRequest(pull);
            if (upstreamPulls.isEmpty()) {
                mergeable.setMergeable(false);
                mergeable.addDescription("Missing any upstream pull request");
                return mergeable;
            }

            for (PullRequest pullRequest : upstreamPulls) {
                if (! helper.isMerged(pullRequest)) {
                    mergeable.setMergeable(false);
                    mergeable.addDescription("Upstream pull request #" + pullRequest.getNumber() + " has not been merged yet");
                }
            }
        } catch (Exception ignore) {
            System.err.printf("Cannot get an upstream pull request of the pull request %d: %s.\n", pull.getNumber(), ignore);
            ignore.printStackTrace(System.err);

            mergeable.setMergeable(false);
            mergeable.addDescription("Cannot get an upstream pull request of the pull request " + pull.getNumber() + ": " + ignore.getMessage());
        }

        if (mergeable.isMergeable()) {
            mergeable.addDescription("Upstream pull request is OK");
        }

        return mergeable;
    }

    protected Result isMergeableByBugzilla(final PullRequest pull, final Set<String> requiredFlags) {
        final Result mergeable = new Result(true);

        final List<Bug> bugs = helper.getBug(pull);
        if (bugs.isEmpty()) {
            mergeable.setMergeable(false);
            mergeable.addDescription("Missing any bugzilla bug");
            return mergeable;
        }

        for (Bug bug : bugs) {
            final Set<String> flagsToCheck = new HashSet<String>(REQUIRED_FLAGS);
            if (requiredFlags != null) {
                flagsToCheck.addAll(requiredFlags);
            }

            final List<Flag> flags = bug.getFlags();
            for (Flag flag : flags) {
                if (flag.getStatus() == Flag.Status.POSITIVE) {
                    flagsToCheck.remove(flag.getName());
                }
            }
            if (! flagsToCheck.isEmpty()) {
                mergeable.setMergeable(false);
                mergeable.addDescription(missingFlagsDescription(bug, flagsToCheck));
            }
        }

        if (mergeable.isMergeable()) {
            mergeable.addDescription("Bugzilla is OK");
        }

        return mergeable;
    }

    private String missingFlagsDescription(Bug bug, Set<String> missingFlags) {
        final StringBuilder description = new StringBuilder("Bug bz").append(bug.getId()).append(" is missing flags");

        String delim = " ";
        for (String missingFlag : missingFlags) {
            description.append(delim).append(missingFlag);
            delim = ", ";
        }
        return description.toString();
    }

}
