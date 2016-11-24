package org.keycloak.client.admin.cli.operations;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientOperations {

    public static List<RoleRepresentation> getRoles(Keycloak client, String realm, ClientRepresentation clientRep) {
        return client.realm(realm).clients().get(clientRep.getId()).roles().list();
    }

    public static ClientRepresentation getForClientId(Keycloak client, String realm, String clientId) {
        List<ClientRepresentation> result = client.realm(realm).clients().findByClientId(clientId);
        if (result.size() > 1) {
            throw new RuntimeException("More than one client returned for clientId: " + clientId);
        }
        return result.size() > 0 ? result.get(0) : null;
    }
}
