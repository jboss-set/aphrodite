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

package org.jboss.set.aphrodite.domain.spi;

import java.util.List;

import org.jboss.set.aphrodite.domain.Payload;

/**
 * @author wangc
 *
 */
public interface PayloadHome {

    /**
     * Retrieve payload by a given name. It accepts Jira fixed version format x.y.z.GA, Bugzilla parent bug alias format
     * eap6420-payload.
     *
     * @param name a string of payload name.
     * @return a <code>Payload</code> instance if given name matches an existing payload, otherwise return null.
     */
    Payload findPayload(String name);

    /**
     * Retrieve all payload by a given project / product name from issue trackers.
     *
     * @return a list of existing <code>Payload</code>
     */
    List<Payload> findAllPayloads();

}
