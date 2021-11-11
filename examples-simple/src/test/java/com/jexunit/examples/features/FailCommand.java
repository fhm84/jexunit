package com.jexunit.examples.features;

import com.jexunit.core.commands.annotation.TestCommand;

import static org.junit.Assert.fail;

/**
 * Test-Command that only fails the test. This is used for testing the fastFail feature.
 *
 * @author Fabian
 */
@TestCommand(value = "fail", fastFail = true)
public class FailCommand {

    public void runFailCommand() {
        fail("Fail command will always fail ;)");
    }

}
