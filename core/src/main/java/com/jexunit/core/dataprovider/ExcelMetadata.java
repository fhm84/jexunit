package com.jexunit.core.dataprovider;

import com.jexunit.core.model.Metadata;

/**
 * Metadata implementation of the ExcelDataProvider. Test test-group will be the excels worksheet, the identifier the
 * row-number of the test-case.
 *
 * @author Fabian
 */
public class ExcelMetadata extends Metadata {

    private int row;

    /**
     * Get the name of the sheet the test-case was defined in.
     *
     * @return the name of the sheet of the test-case
     */
    public String getSheet() {
        return getTestGroup();
    }

    public void setSheet(final String sheet) {
        setTestGroup(sheet);
    }

    /**
     * Get the row-number of the test-case.
     *
     * @return the row-number of the test-case
     */
    public int getRow() {
        return row;
    }

    public void setRow(final int row) {
        this.row = row;
        setIdentifier(String.valueOf(row));
    }

    @Override
    public String getDetailedIdentifier() {
        return String.format("worksheet: %s, row: %s", getTestGroup(), row);
    }

}
