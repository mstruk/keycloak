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

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public enum ResourceType {

    REALMS(RealmRepresentation.class, true),
    REALM(RealmRepresentation.class, false),
    USERS(UserRepresentation.class, true),
    USER(UserRepresentation.class, false),
    ROLES(RoleRepresentation.class, true),
    ROLE(RoleRepresentation.class, false),
    AVAILABLE_ROLES(RoleRepresentation.class, true),
    CLIENTS(ClientRepresentation.class, true),
    CLIENT(ClientRepresentation.class, false);

    private Class clazz;
    private boolean collection;


    public static ResourceType fromTypeName(String option) {
        return valueOf(option.toUpperCase().replace("-", "_"));
    }

    private <T> ResourceType(Class<T> c, boolean collection) {
        this.clazz = c;
        this.collection = collection;
    }

    public Class getType() {
        return clazz;
    }

    public boolean isCollectionType() {
        return collection;
    }

    public String toTypeName() {
        return toString().toLowerCase().replace("_", "-");
    }

    public ResourceHandler newHandler() {
        switch (this) {
            case REALMS:
            case REALM:
                return new RealmResourceHandler(this);
            case USERS:
            case USER:
                return new UserResourceHandler(this);
            case ROLES:
            case ROLE:
                return new RoleResourceHandler(this);
            case AVAILABLE_ROLES:
                return new AvailableRoleResourceHandler(this);
            case CLIENTS:
            case CLIENT:
                return new ClientResourceHandler(this);
            default:
                throw new RuntimeException("Unsupported type: " + toTypeName());
        }
    }
}
