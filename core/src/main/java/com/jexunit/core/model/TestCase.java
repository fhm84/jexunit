package com.jexunit.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the "entity" for a single test instruction. A row consists of a command (for the API) and a number of values
 * for this command. Additionally there is the name of the worksheet and the rownumber of the command for information
 * while debugging and test failures.
 * 
 * @author fabian
 * 
 */
public class TestCase<T extends Metadata> {

	private String testCommand;
	private T metadata;
	private Map<String, TestCell> values = new LinkedHashMap<String, TestCell>();

	private String comment;

	private boolean disabled = false;
	private boolean exceptionExpected = false;
	private boolean fastFail = false;
	private boolean breakpointEnabled = false;

	public TestCase() {
	}

	public TestCase(T metadata) {
		this.metadata = metadata;
	}

	/**
	 * Get the test-command for the test-case.
	 * 
	 * @return the test-command
	 */
	public String getTestCommand() {
		return testCommand;
	}

	public void setTestCommand(String testCommand) {
		this.testCommand = testCommand;
	}

	/**
	 * Get the metadata to the test-case. This will include the test-group, the identifier (inside the test-group) and
	 * possibly other things.
	 * 
	 * @return the metadata of the test-case
	 */
	public T getMetadata() {
		return metadata;
	}

	public void setMetadata(T metadata) {
		this.metadata = metadata;
	}

	/**
	 * Get the values (found/read from the excel file) for the test-case.
	 * 
	 * @return the values for the test-case
	 */
	public Map<String, TestCell> getValues() {
		return values;
	}

	public void setValues(Map<String, TestCell> values) {
		this.values = values;
	}

	/**
	 * It's possible comment the test-command/-case in the data-file (excel file).
	 * 
	 * @return the comment out of the data-file
	 */
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Flag for disabling the test-case.
	 * 
	 * @return true, if the test-case should be disabled, else false (default)
	 */
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Flag for expecting an exception.
	 * 
	 * @return true, if the test-case expects an exception, else false (default)
	 */
	public boolean isExceptionExpected() {
		return exceptionExpected;
	}

	public void setExceptionExpected(boolean exceptionExpected) {
		this.exceptionExpected = exceptionExpected;
	}

	/**
	 * Flag for fast failing the complete test sheet on failing test command.
	 * 
	 * @return true, if the test-case fast fails the complete test sheet, else false (default)
	 */
	public boolean isFastFail() {
		return fastFail;
	}

	public void setFastFail(boolean fastFail) {
		this.fastFail = fastFail;
	}

	/**
	 * Flag for debugging. If this flag is set to true, you can debug your command using conditional breakpoints.
	 * 
	 * @return true, if breakpoint should be enabled for the test-case, else false (default)
	 */
	public boolean isBreakpointEnabled() {
		return breakpointEnabled;
	}

	public void setBreakpointEnabled(boolean breakpointEnabled) {
		this.breakpointEnabled = breakpointEnabled;
	}

	/**
	 * Get the String-representation for the test-case. This will return the test-group (for example the sheet-name),
	 * because it's used for structured "description" of the JUnit test results.
	 */
	@Override
	public String toString() {
		if (metadata != null) {
			return metadata.getTestGroup();
		}
		return super.toString();
	}
}
