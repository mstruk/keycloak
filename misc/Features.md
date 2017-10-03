Adding / Modifying Features
---------------------------

When you add or modify a feature in core/src/main/java/org/keycloak/Feature.java 
you need to make appropriate modifications in several other files in order to keep things
consistent.

The following files need attention:
 - pom.xml (properties in 'community' and 'product' profiles)
 - wildfly/server-subsystem/src/main/config/default-server-subsys-config.properties (<features> section)
 - wildfly/server-subsystem/src/main/resources/subsystem-templates/keycloak-server.xml (<features> section)
 - wildfly/server-subsystem/src/main/resources/cli/default-keycloak-subsys-config.cli
 - wildfly/server-subsystem/src/test/java/org/keycloak/subsystem/server/extension/FeaturesTestCase.java (COMMUNITY_FEATURES, and PRODUCT_FEATURES constants)
 - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-standalone.cli
 - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-standalone-ha.cli
 - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-domain-standalone.cli
 - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-domain-clustered.cli
 - distribution/distribution-tests/src/test/java/org/keycloak/test/distribution/DistributionFeaturesTest.java  (COMMUNITY_FEATURES, and PRODUCT_FEATURES constants)
 
In addition, the following files should be modified to support tests:
 - testsuite/integration-arquillian/tests/pom.xml (systemPropertyVariables of maven-surefire-plugin, and properties of 'community', and 'product' profiles)
 - testsuite/integration-arquillian/tests/base/src/test/resources/META-INF/keycloak-server.json (feature section)
 - testsuite/utils/src/main/resources/META-INF/keycloak-server.json (feature section)
 
 
 Changing a feature from preview to stable
 -----------------------------------------
 
 Preview features have names that end with '-preview'. For example: 'authorization-preview'.
 When a feature is mature enough to be deemed stable we need to remove '-preview' suffix from its name.
 
 In Feature.java there is the constructor:
 ```
   Feature(boolean preview, boolean defaultValue) { ... }
 ```
 
Preview features have the 'preview' argument set to true - which ensures that they automatically get a '-preview' suffix.
Also, make sure to properly change 'defaultValue' argument which marks if feature is enabled or disabled by default.
Note, how that may be distribution dependent - setting this value to Version.IS_COMMUNITY_VERSION will make feature enabled 
by default for 'Keycloak' distribution, but not for product distributions.

Feature name is also present in all the locations mentioned in previous chapter, and has to be changed in all places where it occurs.
Also make sure to properly update the default enabled status in all those places.

Finally, feature name also appears in 
 [features.adoc](https://github.com/keycloak/keycloak-documentation/tree/master/server_installation/features.adoc) and 
 [changes.adoc](https://github.com/keycloak/keycloak-documentation/tree/master/upgrading/topics/keycloak/changes.adoc) which 
 are part of [keycloak-documentation](https://github.com/keycloak/keycloak-documentation) project.
 
These files have to be updated as well. In features.adoc name has to be updated (suffix '-preview' removed, and default enabled status updated), 
and in changed.doc a new chapter has to be added explaining that a preview feature has been upgraded to stable is enabled by default 
where before it was not, and that there is now a new system property to be used.