package com.jexunit.core.commands;

/**
 * Here the default/built-in commands are defined. These are the following ones: disabled, exception, report.
 * 
 * @author fabian
 * 
 */
public enum DefaultCommands {

	/**
	 * Default command for disabling a "test"-worksheet.
	 */
	DISABLED("jexunit.defaultcommand.disabled"),
	/**
	 * Default command for reporting (log something from the excel-file).
	 */
	REPORT("jexunit.defaultcommand.report"),
	/**
	 * Default command/parameter for expecting an exception.
	 */
	EXCEPTION_EXCPECTED("jexunit.defaultcommand.exception_expected"),
	/**
	 * Default command/parameter for "setting a breakpoint inside the excel-file" to be able to debug the test more
	 * easily.
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
