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
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Collects all {@code PullEvaluator} services and redirects to them
 * according to the target github branch of pull requests.
 * One evaluator impl corresponds to one target branch on github.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class ServicePullEvaluator implements PullEvaluator {
    private ServiceLoader<PullEvaluator> serviceLoader = ServiceLoader.load(PullEvaluator.class);

    private boolean initialized = false;

    private final Map<String, PullEvaluator> evaluators = new HashMap<String, PullEvaluator>();

    @Override
    public void init(PullHelper helper, Properties configuration) {
        if (initialized)
            throw new IllegalStateException("ServicePullEvaluator has already been initialized");

        // initiate available evaluators
        for (PullEvaluator evaluator : serviceLoader) {
            evaluator.init(helper, configuration);
            final String evaluatorTargetBranch = evaluator.getTargetBranch();
            if (evaluators.containsKey(evaluatorTargetBranch))
                throw new IllegalStateException("Multiple evaluators dedicated to the branch " + evaluatorTargetBranch);
            evaluators.put(evaluatorTargetBranch, evaluator);
        }

        initialized = true;
    }

    @Override
    public PullEvaluator.Result isMergeable(final PullRequest pull) {
        if (!initialized)
            throw new IllegalStateException("ServicePullEvaluator has not been initialized yet");

        final String targetBranch = pull.getBase().getRef();
        final PullEvaluator evaluator = evaluators.get(targetBranch);

        if (evaluator == null)
            throw new IllegalStateException("Couldn't find any evaluator for target github branch " + targetBranch);

        return evaluator.isMergeable(pull);
    }

    @Override
    public String getTargetBranch() {
        // doesn't make sense for this class
        throw new IllegalStateException("Unsupported operation");
    }

    public Set<String> getCoveredBranches() {
        return new HashSet<String>(evaluators.keySet());
    }

}
