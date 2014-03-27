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

package org.jboss.pull.shared.connectors.common;

import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.IssueHelper;

import java.util.Properties;

/**
 * @author navssurtani
 */

public abstract class AbstractCommonIssueHelper implements IssueHelper {

    protected Properties fromUtil;

    public AbstractCommonIssueHelper(final String configurationFileProperty,
                                     final String configurationFileDefault) throws Exception {

        // We just want to initialise the properties here.
        try {
            this.fromUtil = Util.loadProperties(configurationFileProperty, configurationFileDefault);
        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }

    }


}
