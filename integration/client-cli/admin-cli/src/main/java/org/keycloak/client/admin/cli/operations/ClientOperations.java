package org.keycloak.client.admin.cli.operations;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientOperations {

    public static String create(Keycloak client, String realm, ClientRepresentation representation) {
        Response response = client.realm(realm).clients().create(representation);
        HttpUtil.checkStatusCreated(response);
        return HttpUtil.extractIdFromLocation(response.getLocation().toString());
    }

    public static ClientRepresentation get(Keycloak client, String realm, String idOfClient) {
        return client.realm(realm).clients().get(idOfClient).toRepresentation();
    }

    public static List<ClientRepresentation> getAll(Keycloak client, String realm) {
        return client.realm(realm).clients().findAll();
    }

    public static void update(Keycloak client, String realm, ClientRepresentation representation) {
        client.realm(realm).clients().get(representation.getId()).update(representation);
    }

    public static void delete(Keycloak client, String realm, String idOfClient) {
        client.realm(realm).clients().get(idOfClient).remove();
    }

    public static List<RoleRepresentation> getRoles(Keycloak client, String realm, ClientRepresentation clientRep) {
        return client.realm(realm).clients().get(clientRep.getId()).roles().list();
    }

    public static ClientRepresentation getByClientId(Keycloak client, String realm, String clientId) {
        List<ClientRepresentation> result = client.realm(realm).clients().findByClientId(clientId);
        if (result.size() > 1) {
            throw new RuntimeException("More than one client returned for clientId: " + clientId);
        }
        return result.size() > 0 ? result.get(0) : null;
    }

}
