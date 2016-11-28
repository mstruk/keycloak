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
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmResourceHandler extends ResourceHandler<RealmRepresentation> {

    public RealmResourceHandler(ResourceType type) {
        super(type);
    }

    @Override
    public String create(Keycloak client, String realm, RealmRepresentation representation) {
        return RealmOperations.create(client, representation);
    }

    @Override
    public RealmRepresentation get(Keycloak client, String realm, String id) {
        return RealmOperations.get(client, id);
    }

    @Override
    public void update(Keycloak client, String realm, RealmRepresentation representation) {
        RealmOperations.update(client, representation);
    }

    @Override
    public void delete(Keycloak client, String realm, String id) {
        RealmOperations.delete(client, id);
    }

    @Override
    public List<RealmRepresentation> getMany(Keycloak client, String realm, int offset, int limit, Map<String, String> filter) {
        return RealmOperations.getAll(client);
    }
}
