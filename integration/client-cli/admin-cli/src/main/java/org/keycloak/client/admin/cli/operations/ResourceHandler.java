/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.client.admin.cli.operations;

import org.keycloak.admin.client.Keycloak;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class ResourceHandler<T> {

    private final ResourceType type;

    public ResourceHandler(ResourceType type) {
        this.type = type;
    }

    public ResourceType getType() {
        return type;
    }

    public abstract String create(Keycloak client, String realm, T representation);

    public abstract T get(Keycloak client, String realm, String id);

    public abstract List<T> getMany(Keycloak client, String realm, int offset, int limit, Map<String, String> filter);

    public abstract void update(Keycloak client, String realm, T representation);

    public abstract void delete(Keycloak client, String realm, String id);

    public void parseArgument(String option, Iterator<String> it) {
        throw new IllegalArgumentException("Invalid option: " + option);
    }

    public void processArguments(Keycloak client, String realm) {
        // no-op
    }
}
