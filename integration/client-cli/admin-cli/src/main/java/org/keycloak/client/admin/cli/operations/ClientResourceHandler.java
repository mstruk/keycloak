package org.keycloak.client.admin.cli.operations;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientResourceHandler extends ResourceHandler<ClientRepresentation> {

    public ClientResourceHandler(ResourceType type) {
        super(type);
    }

    @Override
    public String create(Keycloak client, String realm, ClientRepresentation representation) {
        return ClientOperations.create(client, realm, representation);
    }

    @Override
    public ClientRepresentation get(Keycloak client, String realm, String id) {
        return ClientOperations.get(client, realm, id);
    }

    @Override
    public List<ClientRepresentation> getMany(Keycloak client, String realm, int offset, int limit, Map<String, String> filter) {
        return ClientOperations.getAll(client, realm);
    }

    @Override
    public void update(Keycloak client, String realm, ClientRepresentation representation) {
        ClientOperations.update(client, realm, representation);
    }

    @Override
    public void delete(Keycloak client, String realm, String id) {
        ClientOperations.delete(client, realm, id);
    }
}
