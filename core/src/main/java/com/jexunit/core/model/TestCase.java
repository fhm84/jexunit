package com.jexunit.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the "entity" for a single test instruction. A row consists of a command (for the API) and
 * a number of values for this command. Additionally there is the name of the worksheet and the
 * rownumber of the command for information while debugging and test failures.
 * 
 * @author fabian
 * 
 */
public class TestCase {

	private String testCommand;
	private String sheet;
	private int row;
	private Map<String, TestCell> values = new LinkedHashMap<String, TestCell>();

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
	 * Get the name of the sheet the test-case was defined in.
	 * 
	 * @return the name of the sheet of the test-case
	 */
	public String getSheet() {
		return sheet;
	}

	public void setSheet(String sheet) {
		this.sheet = sheet;
	}

	/**
	 * Get the row-number of the test-case.
	 * 
	 * @return the row-number of the test-case
	 */
	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	/**
	 * Get the String-representation for the test-case. This will return the sheet-name, because
	 * it's used for structured "description" of the JUnit test results.
	 */
	@Override
	public String toString() {
		return sheet;
	}
}
