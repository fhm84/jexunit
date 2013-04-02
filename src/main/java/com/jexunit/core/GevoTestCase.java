package com.jexunit.core;

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
public class GevoTestCase {

	private String testCommand;
	private String sheet;
	private int row;
	private Map<String, GevoTestCell> values = new LinkedHashMap<String, GevoTestCell>();

	public String getTestCommand() {
		return testCommand;
	}

	public void setTestCommand(String testCommand) {
		this.testCommand = testCommand;
	}

	public Map<String, GevoTestCell> getValues() {
		return values;
	}

	public void setValues(Map<String, GevoTestCell> values) {
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
