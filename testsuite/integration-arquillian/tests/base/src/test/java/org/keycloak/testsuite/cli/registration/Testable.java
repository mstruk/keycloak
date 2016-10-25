package org.keycloak.testsuite.cli.registration;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@FunctionalInterface
public interface Testable {

    void test() throws Exception;

}
