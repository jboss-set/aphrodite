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

package org.jboss.pull.shared.connectors.bugzilla;


import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.IssueHelper;
import org.jboss.pull.shared.connectors.common.AbstractCommonIssueHelper;
import org.jboss.pull.shared.connectors.common.Issue;

import java.net.URL;

public class BZHelper extends AbstractCommonIssueHelper implements IssueHelper {

    private static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/";

    private final String BUGZILLA_LOGIN;
    private final String BUGZILLA_PASSWORD;

    private final Bugzilla bugzillaClient;

    public BZHelper(final String configurationFileProperty, final String configurationFileDefault) throws Exception {
        super(configurationFileProperty, configurationFileDefault);
        try {
            BUGZILLA_LOGIN = Util.require(fromUtil, "bugzilla.login");
            BUGZILLA_PASSWORD = Util.require(fromUtil, "bugzilla.password");

            // initialize bugzilla client
            bugzillaClient = new Bugzilla(BUGZILLA_BASE, BUGZILLA_LOGIN, BUGZILLA_PASSWORD);
        } catch (Exception e) {
            System.err.printf("Cannot initialize: %s\n", e);
            e.printStackTrace(System.err);
            throw e;
        }
    }

    @Override
    public Issue findIssue(URL url) throws IllegalArgumentException {
        return bugzillaClient.getBug(cutIdFromURL(url));
    }

    @Override
    public boolean accepts(URL url) {
        return url.getHost().equals(BUGZILLA_BASE);
    }

    // FIXME: This has to be implemented properly.
    @Override
    public boolean updateStatus(URL url, Enum status) {
        throw new UnsupportedOperationException("This feature is not implemented yet.");
    }

    private Integer cutIdFromURL(URL url) {
        return null;
    }
}
