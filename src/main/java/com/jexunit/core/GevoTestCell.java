package com.jexunit.core;

/**
 * The "entity" for a single cell. It consists of the value and the column (number).
 * 
 * @author fabian
 * 
 */
public class GevoTestCell {

	// Inhalt der Zelle
	private String value;

	private int column;

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
		return GevoTestExcelLoader.getColumn(column);
	}

	public void setColumn(int column) {
		this.column = column;
	}
}
