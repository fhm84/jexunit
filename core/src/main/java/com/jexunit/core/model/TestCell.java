package com.jexunit.core.model;

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

    private String identifier;

    /**
     * Default constructor.
     */
    public TestCell() {
    }

    /**
     * Constructor setting the column number and value.
     *
     * @param identifier the number representing the column
     * @param value      the cells value
     */
    public TestCell(final String identifier, final String value) {
        this.identifier = identifier;
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

    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Returns the column like it is displayed in excel (A, B, C, ...).
     *
     * @return the name of the column (A, B, C, ...)
     */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

}
