package com.jexunit.core.commands;

/**
 * Here the default/built-in commands and command-attributes are defined. These are the following ones: disabled,
 * exception_expected, report, breakpoint, comment.
 * 
 * @author fabian
 * 
 */
public enum DefaultCommands {

	/**
	 * Default command/parameter for disabling a "test"-worksheet.
	 */
	DISABLED("jexunit.defaultcommand.disabled"),

	/**
	 * Default command for reporting (log something from the excel-file).
	 */
	REPORT("jexunit.defaultcommand.report"),

	/**
	 * Parameter for expecting an exception.
	 */
	EXCEPTION_EXCPECTED("jexunit.defaultcommand.exception_expected"),

	/**
	 * Parameter for a comment in case the assertion/command fails. This parameter can be used like the message for
	 * asserts.
	 */
	COMMENT("jexunit.defaultcommand.comment"),

	/**
	 * Parameter for a command to fast fail the complete test sheet. This parameter will override the default setting of
	 * the test command.
	 */
	FAST_FAIL("jexunit.defaultcommand.fastfail"),

	/**
	 * Parameter for "setting a breakpoint inside the excel-file" to be able to debug the test more easily.
	 */
	BREAKPOINT("jexunit.defaultcommand.breakpoint");

	private String configKey;

	private DefaultCommands(String configKey) {
		this.configKey = configKey;
	}

	public String getConfigKey() {
		return configKey;
	}

}
