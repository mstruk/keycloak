package org.keycloak.client.admin.cli.commands;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserOperations {

    public static String create(Keycloak client, String realm, UserRepresentation user) {
        Response response = client.realm(realm).users().create(user);
        HttpUtil.checkStatusCreated(response);
        return HttpUtil.extractIdFromLocation(response.getLocation().toString());
    }

    public static UserRepresentation get(Keycloak client, String realm, String id) {
        return client.realm(realm).users().get(id).toRepresentation();
    }

    public static void update(Keycloak client, String realm, UserRepresentation user) {
        if (user.getId() == null) {
            throw new RuntimeException("User has no id set");
        }
        client.realm(realm).users().get(user.getId()).update(user);
    }

    public static void delete(Keycloak client, String realm, String id) {
        Response response = client.realm(realm).users().delete(id);
        HttpUtil.checkStatusNoContent(response);
    }
}
