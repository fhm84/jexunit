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
     * Path of the excel file the test-case was defined in (as given in {@link ExcelFile}).
     */
    private String file;

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
     * Get the path of the excel file the test-case was defined in.
     *
     * @return the path of the excel file (as given in {@link ExcelFile}), or <code>null</code> if not set
     */
    public String getFile() {
        return file;
    }

    public void setFile(final String file) {
        this.file = file;
    }

    @Override
    public String getDetailedIdentifier() {
        if (file == null) {
            return String.format("worksheet: %s, cell: %s", getTestGroup(), getIdentifier());
        }
        return String.format("file: %s, worksheet: %s, cell: %s", file, getTestGroup(), getIdentifier());
    }

}
