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
import org.jboss.pull.shared.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * An evaluator based on Bugzilla flags resolution.
 * It can be configured to which flags are needed in order
 * to merge a pull request.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class BugFlagBasedPullEvaluator extends BasePullEvaluator {
    public static final String REQUIRED_FLAGS_PROPERTY = "required.flags";

    public static final String PM_ACK = "pm_ack";
    public static final String QA_ACK = "qa_ack";
    public static final String DEVEL_ACK = "devel_ack";

    protected final Set<String> REQUIRED_FLAGS = new HashSet<String>();

    @Override
    public void init(final PullHelper helper, final Properties configuration, final String version) {
        super.init(helper, configuration, version);

        REQUIRED_FLAGS.add(PM_ACK);
        REQUIRED_FLAGS.add(DEVEL_ACK);
        REQUIRED_FLAGS.add(QA_ACK);

        final String requiredFlags = Util.require(configuration, version + "." + REQUIRED_FLAGS_PROPERTY);
        final StringTokenizer tokenizer = new StringTokenizer(requiredFlags, ", ");
        while (tokenizer.hasMoreTokens()) {
            final String requiredFlag = tokenizer.nextToken();
            REQUIRED_FLAGS.add(requiredFlag);
        }
    }

    @Override
    public Result isMergeable(final PullRequest pull) {
        final Result mergeable;
        mergeable = super.isMergeable(pull);
        mergeable.and(isMergeableByBugzilla(pull));
        return mergeable;
    }

    protected Result isMergeableByBugzilla(final PullRequest pull) {
        final Result mergeable = new Result(true);

        final List<Bug> bugs = (List<Bug>) getIssue(pull);
        if (bugs.isEmpty()) {
            mergeable.setMergeable(false);
            mergeable.addDescription("- Missing any bugzilla bug");
            return mergeable;
        }

        for (Bug bug : bugs) {
            final Set<String> flagsToCheck = new HashSet<String>(this.REQUIRED_FLAGS);

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
            mergeable.addDescription("+ Bugzilla is OK");
        }

        return mergeable;
    }

    private String missingFlagsDescription(Bug bug, Set<String> missingFlags) {
        final StringBuilder description = new StringBuilder("- Bug bz").append(bug.getNumber()).append(" is missing flags");

        String delim = " ";
        for (String missingFlag : missingFlags) {
            description.append(delim).append(missingFlag);
            delim = ", ";
        }
        return description.toString();
    }

}
