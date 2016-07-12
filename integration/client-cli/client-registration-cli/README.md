Keycloak Client Registration CLI
================================

This is a tool specifically designed to interact with Keycloak server REST endpoints for registering new client applications.

You can read more about client registration endpoints in [Securing Client Applications Guide](https://keycloak.gitbooks.io/securing-client-applications-guide/content/v/2.0/topics/client-registration.html).

It is necessary to create a new client configuration for each new application hosted on a unique hostname in order for Keycloak to be able to interact with the application and perform its function of providing login page, SSO session management etc.

Normally a realm administrator performs client configuration by using Keycloak Admin Console - a web UI.

This tool allows you to configure clients from a command line. It can be used from within shell scripts as well.

If you are using Windows, you may want to read a [version of this document for Windows](README-windows.md).


Building
--------

In Keycloak project's root directory (let's call it KEYCLOAK_HOME) run the following commands:
 
    $ mvn clean install -f integration/client-cli/pom.xml


If build fails due to missing artifacts, you need to perform a full build first:

    $ mvn clean install -DskipTests


Installing
----------

Change to the directory where you want to install the client.

Unzip the built distribution:

    $ unzip $KEYCLOAK_HOME/integration/client-cli/client-cli-dist/target/keycloak-client-cli-*.zip

Add the directory to your PATH:

    $ export PATH=$PATH:$PWD/keycloak-client-tools/bin
    
Make sure the client works:

    $ kcreg.sh
    


Using
-----

Start up a local Keycloak Server or use an existing running server.

First login as an existing user:

    $ kcreg.sh config credentials --server http://localhost:8080/auth --realm master --user admin

You will be prompted for a password. If you're connecting to a different server adjust the server endpoint url, the realm and user accordingly.

Create a new client:
```
$ kcreg.sh create -f - << EOF
{
  "clientId": "examples-admin-client",
  "directAccessGrantsEnabled": true,
  "enabled": true,
  "fullScopeAllowed": true,
  "baseUrl": "/examples-admin-client",
  "redirectUris": [
    "/examples-admin-client/*"
  ],
  "secret": "password"
}
EOF

```

If you as a user don't have permissions to manage clients but were given an Initial Access Token by your realm administrator, you can use that for authorization:

    $ kcreg.sh create -f my_client.json -t -

This will prompt you for Initial Access Token.

File my_client.json can be very simple:
```
{
  "clientId": "my_client"
}
```

You can delete the client by using:

    $ kcreg.sh delete my_client
    
For scripting purposes you can have the tool print out no human readable message, but just the ClientId of the created new client (by using -i):

```
CLIENTID=$(kcreg.sh create -i -f my_client.json)
echo $CLIENTID
```

Now we can display the client we just created:

    $ kcreg.sh get examples-admin-client

Save it to a local file:

    $ kcreg.sh get examples-admin-client > examples-admin-client.json

Use text editor, and delete all occurrences of 'id' property from the file.

Now we can use this file as a template to create other clients:

    $ kcreg.sh create -f examples-admin-client.json -s clientId=examples-jee-client -s baseUrl=/examples-jee-client -s 'redirectUris=["/examples-jee-client/*"]'
    $ kcreg.sh get examples-jee-client
    
We can also get client configuration in a standard OIDC format, rather than in Keycloak default format:

    $ kcreg.sh get examples-jee-client -e oidc
    
After creating a new client we need to configure our application for this client. We can get the adapter configuration by using 'install' endpoint:

    $ kcreg.sh get examples-jee-client -e install

We may need to update our client after it was created. For example we may want to disable it:

    $ kcreg.sh update examples-jee-client -s enabled=false
    
This would fetch the current configuration from the server, update it with the new value, and push it back to the server.

We may sometimes want to fetch the configuration, change it with a text editor and push it back to the server:

    $ kcreg.sh update examples-jee-client -f new-examples-jee-client.json -s enabled=true

This would take configuration from the file, apply additional changes to it, and then overwrite existing server-side configuration with the new one.

Or we may want to update the configuration using OIDC format:
 
    $ kcreg.sh update examples-jee-client -e oidc -s client_uri=/examples-jee -s 'redirect_uris=["/examples-jee/*"]'

We can also create clients using SAML2 Metadata SPSSODescriptor format:

    $ kcreg.sh create -f saml-sp-metadata.xml

Client registration tool automatically detects the format and uses the appropriate server endpoint url.
    
If you get a 'Client Identifier in use [invalid_client_metadata]' error it means a client was already created with the same entityID as used in saml-sp-metadata.xml.

Once a client is created that way it can then be updated using the default JSON format.

We can also delete the client using its clientId:

    $ kcreg.sh delete examples-jee-client
    
When you as a user don't have permissions to manage clients, and have to use Initial Access Tokens, any operations after the creation of a client require a valid Registration Access Token.
 
Client registration tool automatically handles these tokens behind the scenes. But it can happen that the token you have is no longer valid, and the realm administrator issues you another Registration Access Token.

You then need to provide it to kcreg command:
 
    $ kcreg.sh config registration-token --client examples-jee-client --server http://localhost:8080/auth --realm master
    
You will be prompted to enter the new token.

If you have client management permissions you can reissue and set the new token directly:

    $ kcreg.sh update-token examples-admin-client


For more information try integrated help:

    $ kcreg.sh help


Running tests
-------------

Integration tests for Client Registration CLI can be found in `testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite/cli/registration`.

They are executed together with other integration tests when running:

    $ mvn clean install -f testsuite/integration-arquillian

If you only want to execute Client Registration CLI tests run the following:

    $ mvn clean install -f testsuite/integration-arquillian '-Dtest=KcReg*'


If you want to see more output from these tests, add `-Dcli.log.output=true`.

Some of the tests currently suffer from intermittent test failures, and are excluded by default. 

To include those tests as well add `-Dtest.intermittent=true`.

There is a test that requires secure connection to Keycloak server, and is skipped by default.

That test can be run individually using the following command:

    $ mvn clean install -f testsuite/integration-arquillian -Pauth-server-wildfly -Dauth.server.ssl.required=true -Dauth.server.log.check=false -Dcli.log.output=true -Dtest.intermittent=true -Dtest=KcRegTruststoreTest

When running tests make sure to shutdown any other instances of Keycloak / Wildfly you may have running on the system or within your IDE.