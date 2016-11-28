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
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserResourceHandler extends ResourceHandler<UserRepresentation> {

    public UserResourceHandler(ResourceType type) {
        super(type);
    }

    @Override
    public String create(Keycloak client, String realm, UserRepresentation representation) {
        return UserOperations.create(client, realm, representation);
    }

    @Override
    public UserRepresentation get(Keycloak client, String realm, String id) {
        return UserOperations.get(client, realm, id);
    }

    @Override
    public void update(Keycloak client, String realm, UserRepresentation representation) {
        UserOperations.update(client, realm, representation);
    }

    @Override
    public void delete(Keycloak client, String realm, String id) {
        UserOperations.delete(client, realm, id);
    }

    @Override
    public List<UserRepresentation> getMany(Keycloak client, String realm, int offset, int limit, Map filter) {
        return UserOperations.getAllFiltered(client, realm, offset, limit, filter);
    }
}
