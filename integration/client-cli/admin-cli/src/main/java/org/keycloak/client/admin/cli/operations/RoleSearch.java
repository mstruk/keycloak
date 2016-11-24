package org.keycloak.client.admin.cli.operations;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RoleSearch {

    private Map<String, RoleRepresentation> rolesByName = new HashMap<>();
    private Map<String, RoleRepresentation> rolesById = new HashMap<>();

    public RoleSearch(List<RoleRepresentation> roles) {
        for (RoleRepresentation r: roles) {
            rolesById.put(r.getId(), r);
            rolesByName.put(r.getName(), r);
        }
    }

    public RoleRepresentation findById(String id) {
        return rolesById.get(id);
    }

    public RoleRepresentation findByName(String name) {
        return rolesByName.get(name);
    }
}
