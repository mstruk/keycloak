package org.keycloak.testsuite.cli.admin;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.cli.KcAdmExec;

import static org.keycloak.testsuite.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmTest extends AbstractAdmCliTest {

    @Test
    public void testBadCommand() {
        /*
         *  Test most basic execution with non-existent command
         */
        KcAdmExec exe = execute("nonexistent");

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Unknown command: nonexistent", exe.stderrLines().get(0));
    }
}
