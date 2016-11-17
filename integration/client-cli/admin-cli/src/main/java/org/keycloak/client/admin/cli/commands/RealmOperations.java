package org.keycloak.client.admin.cli.commands;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RealmOperations {

    public static String create(Keycloak client, RealmRepresentation realm) {
        client.realms().create(realm);
        return realm.getRealm();
    }

    public static RealmRepresentation get(Keycloak client, String name) {
        return client.realm(name).toRepresentation();
    }

    public static void update(Keycloak client, RealmRepresentation realm) {
        client.realm(realm.getRealm()).update(realm);
    }

    public static void delete(Keycloak client, String name) {
        client.realm(name).remove();
    }

}
