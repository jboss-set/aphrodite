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

package org.jboss.set.aphrodite.issue.trackers.common;

import org.apache.commons.logging.Log;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.AphroditeConfig;
import org.jboss.set.aphrodite.config.IssueTrackerConfig;
import org.jboss.set.aphrodite.spi.IssueTrackerService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * An abstract IssueTracker which provides logic common to all issue trackers.
 *
 * @author Ryan Emerson
 */
public abstract class AbstractIssueTracker implements IssueTrackerService {
    protected final String TRACKER_TYPE;
    protected IssueTrackerConfig config;
    protected URL baseUrl;

    protected abstract Log getLog();

    public AbstractIssueTracker(String TRACKER_TYPE) {
        this.TRACKER_TYPE = TRACKER_TYPE;
    }

    @Override
    public boolean init(AphroditeConfig aphroditeConfig) {
        Iterator<IssueTrackerConfig> i = aphroditeConfig.getIssueTrackerConfigs().iterator();
        while (i.hasNext()) {
            IssueTrackerConfig config = i.next();
            if (config.getTracker().equalsIgnoreCase(TRACKER_TYPE)) {
                i.remove(); // Remove so that this service cannot be instantiated twice
                return init(config);
            }
        }
        return false;
    }

    @Override
    public boolean init(IssueTrackerConfig config) {
        this.config = config;
        String url = config.getUrl();
        if (!url.endsWith("/"))
            url = url + "/";

        try {
            baseUrl = new URL(config.getUrl());
        } catch (MalformedURLException e) {
            String errorMsg = "Invalid tracker url '" + url + "'. " + this.getClass().getName() +
                    " service for '" + url + "' cannot be started";
            Utils.logException(getLog(), errorMsg, e);
            return false;
        }
        return true;
    }
}
