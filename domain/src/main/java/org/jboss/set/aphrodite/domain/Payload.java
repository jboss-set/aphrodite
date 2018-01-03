/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.aphrodite.domain;

import java.net.URL;
import java.util.List;

/**
 * @author wangc
 *
 */
public interface Payload {

    /**
     * Retrieve the name of payload. In Jira, it's represented by the fixed version name which defines payload e.g. "7.1.1.GA".
     * In Bugzilla, it's represented by the string summary of the parent tracker bug. e.g. "EAP 6.4.15 (CP15) Payload Tracker"
     *
     * @return the name of payload.
     */
    String getName();

    /**
     * Retrieve the payload relevant URL link. In Jira, it's the version link e.g.
     * https://issues.jboss.org/projects/JBEAP/versions/12332890. In Bugzilla, it's the parent tracker bug URL e.g.
     * https://bugzilla.redhat.com/show_bug.cgi?id=1510090
     *
     * @return the URL of payload, null in case of MalformedURLException.
     */
    URL getUrl();

    /**
     * Retrieve all issues associated with current payload.
     *
     * @return a list of issues in payload
     */
    List<? extends Issue> getIssues();

    /**
     * Get the payload size.
     *
     * @return an integer represents payload size.
     */
    int getSize();

    /**
     * Check if a given payload is released. Jira payload is released if the associated version is released. Bugzilla payload is
     * released if the parent tracker bug is VERIFIED or CLOSED.
     *
     * @return true if the payload is released, otherwise false.
     */
    boolean isReleased();

}
