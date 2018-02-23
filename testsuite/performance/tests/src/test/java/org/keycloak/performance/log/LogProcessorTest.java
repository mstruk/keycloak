package org.keycloak.performance.log;

import org.junit.Test;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class LogProcessorTest {

    @Test
    public void testExtract() {
        LogProcessor.main(new String [] {"--file", "src/test/resources/log/simulation_1.log", "-e",
                "-o", "target/filtered_simulation.log", "--start", "1518642779098", "--end", "1518642800359"});
    }

    @Test
    public void testExtractWithCompleteSession() {
        LogProcessor.main(new String [] {"--file", "src/test/resources/log/simulation_1.log", "-e",
                "-o", "target/filtered_simulation.log", "--start", "1518642779098", "--end", "1518642800359", "--completeSessions"});
    }

}
