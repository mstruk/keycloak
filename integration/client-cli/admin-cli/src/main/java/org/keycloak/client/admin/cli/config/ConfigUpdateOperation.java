package org.keycloak.client.admin.cli.config;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public interface  ConfigUpdateOperation {

    void update(ConfigData data);

}
