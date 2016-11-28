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
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RoleOperations {

    public static String create(Keycloak client, String realm, RoleRepresentation role) {
        client.realm(realm).roles().create(role);
        return role.getName();
    }

    public static RoleRepresentation get(Keycloak client, String realm, String name) {
        return client.realm(realm).roles().get(name).toRepresentation();
    }

    public static List<RoleRepresentation> getAll(Keycloak client, String realm) {
        return client.realm(realm).roles().list();
    }

    public static List<RoleRepresentation> getClientRolesForUser(Keycloak client, String realm, String userid, String clientid) {
        return client.realm(realm).users().get(userid).roles().clientLevel(clientid).listAll();
    }

    public static List<RoleRepresentation> getAvailableClientRolesForUser(Keycloak client, String realm, String userid, String clientid) {
        return client.realm(realm).users().get(userid).roles().clientLevel(clientid).listAvailable();
    }

    public static void update(Keycloak client, String realm, RoleRepresentation role) {
        client.realm(realm).roles().get(role.getName()).update(role);
    }

    public static void delete(Keycloak client, String realm, String name) {
        client.realm(realm).roles().deleteRole(name);
    }

}
