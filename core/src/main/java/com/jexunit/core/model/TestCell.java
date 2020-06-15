package com.jexunit.core.model;

import com.jexunit.core.dataprovider.ExcelLoader;

/**
 * The "entity" for a single cell. It consists of the value and the column (number).
 *
 * @author fabian
 */
public class TestCell {

    /**
     * the cells value
     */
    private String value;

    // FIXME: maybe this should be renamed? -> 'identifier'? 'column' focuses on excel files
    private int column;

    /**
     * Default constructor.
     */
    public TestCell() {
    }

    /**
     * Constructor setting the column number and value.
     *
     * @param column the number representing the column
     * @param value  the cells value
     */
    public TestCell(final int column, final String value) {
        this.column = column;
        this.value = value;
    }

    /**
     * Get the cells value.
     *
     * @return the value of the cell
     */
    public String getValue() {
        return value;
    }

    public void setvalue(final String value) {
        this.value = value;
    }

    /**
     * Returns the column like it is displayed in excel (A, B, C, ...).
     *
     * @return the name of the column (A, B, C, ...)
     */
    public String getColumn() {
        // TODO: remove "dependency" to the excel loader here!
        return ExcelLoader.getColumn(column);
    }

    public void setColumn(final int column) {
        this.column = column;
    }

}
