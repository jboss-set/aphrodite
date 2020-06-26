/*
 * Copyright 2020 Red Hat, Inc.
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

public class VersionUpgrade {
    private String componentId;
    private String oldVersion;
    private String newVersion;

    public VersionUpgrade(String componentId, String oldVersion, String newVersion) {
        this.componentId = componentId;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    @Override
    public String toString() {
        return "VersionUpgrade{" +
                "componentId='" + componentId + '\'' +
                ", oldVersion='" + oldVersion + '\'' +
                ", newVersion='" + newVersion + '\'' +
                '}';
    }
}
