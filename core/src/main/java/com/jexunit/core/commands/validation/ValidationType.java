package com.jexunit.core.commands.validation;

/**
 * Validation type for command valdation.
 */
public enum ValidationType {

	/**
	 * Do not validate the test-data before execution. This may cause test failures because of not existing command
	 * implementation.
	 */
	IGNORE,

	/**
	 * (default) Log a warning if no command implementation found for command. This will remove the command from
	 * execution list to not fail the test run because of missing command implementation.
	 */
	WARN,

	/**
	 * (Fast) Fail test execution in validation "phase", before executing the commands.
	 */
	FAIL;

}
