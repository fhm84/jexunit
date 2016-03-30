package com.jexunit.examples.features;

import static org.junit.Assert.assertTrue;

import com.jexunit.core.commands.annotation.TestCommand;

/**
 * Test-Command that only fails the test. This is used for testing the fastFail feature.
 * 
 * @author Fabian
 *
 */
@TestCommand(value = "fail", fastFail = true)
public class FailCommand {

	public void runFailCommand() {
		assertTrue("Fail command will always fail ;)", false);
	}
}
