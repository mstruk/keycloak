package org.keycloak.client.admin.cli.operations;

import org.keycloak.client.admin.cli.operations.RoleOperations.LIST_OF_ROLES;
import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.Pair;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientOperations {

    public static String getIdFromClientId(String rootUrl, String realm, String auth, String clientId) {

        String resourceUrl = composeResourceUrl(rootUrl, realm, "clients");

        resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, "clientId", clientId, "first", "0", "max", "2");

        LinkedList<Pair> headers = new LinkedList<>();
        if (auth != null) {
            headers.add(new Pair("Authorization", auth));
        }
        headers.add(new Pair("Accept", "application/json"));

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest("get", resourceUrl, new HeadersBody(headers));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: GET " + resourceUrl, e);
        }

        response.checkSuccess();

        List<ClientRepresentation> clients;
        try {
            clients = JsonSerialization.readValue(response.getBody(), new ArrayList<ClientRepresentation>(){}.getClass());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON response", e);
        }

        if (clients.size() > 1) {
            throw new RuntimeException("Multiple clients found for clientId: " + clientId);
        }

        if (clients.size() == 0) {
            throw new RuntimeException("Client not found for clientId: " + clientId);
        }

        return clients.get(0).getId();
    }

    public static List<RoleRepresentation> getRolesForClient(String rootUrl, String realm, String auth, String idOfClient) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "clients/" + idOfClient + "/roles");

        LinkedList<Pair> headers = new LinkedList<>();
        if (auth != null) {
            headers.add(new Pair("Authorization", auth));
        }
        headers.add(new Pair("Accept", "application/json"));

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest("get", resourceUrl, new HeadersBody(headers));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: GET " + resourceUrl, e);
        }

        response.checkSuccess();

        List<RoleRepresentation> roles;
        try {
            roles = JsonSerialization.readValue(response.getBody(), LIST_OF_ROLES.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON response", e);
        }

        return roles;
    }
}
