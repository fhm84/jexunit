package com.jexunit.core.commands.validation;

import java.util.Arrays;

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

	/**
	 * Get the validation type for the given (configured) value, ignoring case and surrounding whitespace.
	 *
	 * @param value the configured value
	 * @return the validation type
	 * @throws IllegalArgumentException if the value is no valid validation type
	 */
	public static ValidationType parse(final String value) {
		if (value != null) {
			for (final ValidationType type : values()) {
				if (type.name().equalsIgnoreCase(value.trim())) {
					return type;
				}
			}
		}
		throw new IllegalArgumentException(String.format("Invalid command validation type '%s', allowed values: %s",
				value, Arrays.toString(values())));
	}

}
