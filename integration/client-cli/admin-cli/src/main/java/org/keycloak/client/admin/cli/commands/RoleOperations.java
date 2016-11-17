package org.keycloak.client.admin.cli.commands;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;

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

    public static void update(Keycloak client, String realm, RoleRepresentation role) {
        //if (role.getId() == null) {
        //    throw new RuntimeException("Role has no id set");
        //}
        client.realm(realm).roles().get(role.getName()).update(role);
    }

    public static void delete(Keycloak client, String realm, String name) {
        client.realm(realm).roles().deleteRole(name);
    }

}
