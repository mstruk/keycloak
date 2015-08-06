Documentation Distribution
==========================

All the documentation, including javadoc, REST API documentation, and a userguide is packaged in a single _keycloak-docs-VERSION-all.zip_ file.

 
Additionally, Swagger based REST API documentation is provided in ready-to-access-through-a-web-server form - as a deployable java web archive in a file _keycloak-docs-VERSION-restapi.war_.

In order to view REST API documentation, perform the following steps:

### Start Keycloak server
    $KEYCLOAK_HOME/bin/standalone.sh

### Start Wildfly client shell
    $KEYCLOAK_HOME/bin/jboss-cli.sh

### Connect to local Keycloak server
    connect

### Deploy REST API documentation .war
    deploy target/keycloak-docs-*-restapi.war


Now you should be able to access REST API documentation at [http://localhost:8080/apidocs](http://localhost:8080/apidocs)
