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

/**
 * An abstract evaluator based on a parent Bugzilla bug resolution.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractParentBugBasedPullEvaluator extends BasePullEvaluator {

    protected final Set<Integer> requiredParents = new HashSet<Integer>();

    /**
     * Override this to define property names of parent bugs required.
     * @return property names of required parent bugs
     */
    protected abstract String[] parentBugNames();


    @Override
    public void init(final PullHelper helper, final Properties configuration) {
        super.init(helper, configuration);

        final String[] parentBugProperties = parentBugNames();
        for (String parentBugProperty : parentBugProperties) {
            requiredParents.add(Integer.valueOf(Util.require(configuration, parentBugProperty)));
        }
    }

    @Override
    public Result isMergeable(final PullRequest pull) {
        final Result mergeable;
        mergeable = isMergeableByUpstream(pull);
        mergeable.and(isMergeableByBugzilla(pull));
        return mergeable;
    }

    protected Result isMergeableByBugzilla(final PullRequest pull) {
        final Result mergeable = new Result(false);

        final List<Bug> bugs = helper.getBug(pull);
        if (bugs.isEmpty()) {
            mergeable.addDescription("Missing any bugzilla bug");
            return mergeable;
        }

        // any referenced (blocked) bug has to...
        for (Bug bug : bugs) {
            final Set<Integer> blocks = bug.getBlocks();
            if (blocks == null || blocks.isEmpty())
                continue;

            // ...contain at least one of the required parent bugs
            boolean hit = false;
            for (Integer parentBug : requiredParents) {
                if (blocks.contains(parentBug))
                    hit = true;
            }

            mergeable.setMergeable(mergeable.isMergeable() || hit);
        }

        if (mergeable.isMergeable()) {
            mergeable.addDescription("Bugzilla is OK");
        } else {
            mergeable.addDescription(missingParentsDescription());
        }

        return mergeable;
    }

    private String missingParentsDescription() {
        final StringBuilder description = new StringBuilder("Referenced bugs should block at least one of these ");

        String delim = " ";
        for (Integer requiredParent : requiredParents) {
            description.append(delim).append("bz").append(requiredParent);
            delim = ", ";
        }
        return description.toString();
    }

}
