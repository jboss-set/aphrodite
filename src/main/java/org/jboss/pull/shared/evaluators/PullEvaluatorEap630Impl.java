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

import org.jboss.pull.shared.Util;

import java.util.Properties;

/**
 * A pull request evaluator for EAP 6.3.0 version.
 * It is based on Bugzilla flag resolution.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class PullEvaluatorEap630Impl extends AbstractFlagBasedPullEvaluator {

    @Override
    protected String getGithubBranchPropertyName() {
        return "eap630.github.branch";
    }

    @Override
    protected String[] additionalRequiredFlags(final Properties properties) {
        return new String[] {
                Util.require(properties, "eap63.version.flag"),
                Util.require(properties, "eap62.version.flag")
        };
    }

}
