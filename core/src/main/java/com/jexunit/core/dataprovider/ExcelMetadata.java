package com.jexunit.core.dataprovider;

import com.jexunit.core.model.Metadata;

/**
 * Metadata implementation of the ExcelDataProvider. Test test-group will be the excels worksheet, the identifier the
 * row-number of the test-case.
 *
 * @author Fabian
 */
public class ExcelMetadata extends Metadata {

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

    @Override
    public String getDetailedIdentifier() {
        return String.format("worksheet: %s, cell: %s", getTestGroup(), getIdentifier());
    }

}
