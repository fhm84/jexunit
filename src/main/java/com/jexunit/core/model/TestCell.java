package com.jexunit.core.model;

import com.jexunit.core.data.ExcelLoader;

/**
 * The "entity" for a single cell. It consists of the value and the column (number).
 * 
 * @author fabian
 * 
 */
public class TestCell {

	// the cells value
	private String value;

	private int column;

	/**
	 * Get the cells value.
	 * 
	 * @return the value of the cell
	 */
	public String getValue() {
		return value;
	}

	public void setvalue(String value) {
		this.value = value;
	}

	/**
	 * Returns the column like it is displayed in excel (A, B, C, ...).
	 * 
	 * @return the name of the column (A, B, C, ...)
	 */
	public String getColumn() {
		return ExcelLoader.getColumn(column);
	}

	public void setColumn(int column) {
		this.column = column;
	}
}
