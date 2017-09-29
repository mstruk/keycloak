Adding / Modifying Features
---------------------------

When you add or modify a feature in core/src/main/java/org/keycloak/Feature.java 
you need to make appropriate modifications in several other files in order to keep things
consistent.

The following files need taking a look at:

 - pom.xml (keycloak-parent - properties in 'community' and 'product' profiles)
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
 - testsuite/integration-arquillian/tests/base/src/test/resources/META-INF/keycloak-server.json (feature section)
 - testsuite/utils/src/main/resources/META-INF/keycloak-server.json (feature section)
 
 
 
