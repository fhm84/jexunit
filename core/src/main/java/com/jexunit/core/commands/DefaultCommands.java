package com.jexunit.core.commands;

/**
 * Here the default/built-in commands are defined. These are the following ones: disabled,
 * exception, report.
 * 
 * @author fabian
 * 
 */
public enum DefaultCommands {

	/**
	 * Default command for disabling a "test"-worksheet.
	 */
	DISABLED("disabled"),
	/**
	 * Default command for reporting (log something from the excel-file).
	 */
	REPORT("report"),
	/**
	 * Default command/parameter for expecting an exception.
	 */
	EXCEPTION_EXCPECTED("exception"),
	/**
	 * Default command/parameter for "setting a breakpoint inside the excel-file" to be able to
	 * debug the test more easily.
	 */
	BREAKPOINT("breakpoint");

	private String commandName;

	private DefaultCommands(String commandName) {
		this.commandName = commandName;
	}

	public String getCommandName() {
		return commandName;
	}

}
