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

	public String getTestCommand() {
		return testCommand;
	}

	public void setTestCommand(String testCommand) {
		this.testCommand = testCommand;
	}

	public Map<String, TestCell> getValues() {
		return values;
	}

	public void setValues(Map<String, TestCell> values) {
		this.values = values;
	}

	public String getSheet() {
		return sheet;
	}

	public void setSheet(String sheet) {
		this.sheet = sheet;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	@Override
	public String toString() {
		return sheet;
	}
}
