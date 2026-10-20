package com.jexunit.core.jupiter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Helper for creating (temporary) excel files for tests running complete worksheets.
 */
final class ExcelTestFile {

    private ExcelTestFile() {
    }

    /**
     * Create a temporary excel file with the given worksheets (named "sheet1", "sheet2", ...).
     *
     * @param sheets the cell values per line for each worksheet (<code>null</code> for a not existing cell)
     * @return the absolute path of the excel file
     */
    static String create(final String[][]... sheets) {
        try {
            final File file = File.createTempFile("jexunit", ".xlsx");
            file.deleteOnExit();
            try (final XSSFWorkbook workbook = new XSSFWorkbook(); final OutputStream out = new FileOutputStream(file)) {
                for (int s = 0; s < sheets.length; s++) {
                    final Sheet sheet = workbook.createSheet("sheet" + (s + 1));
                    for (int i = 0; i < sheets[s].length; i++) {
                        final Row row = sheet.createRow(i);
                        for (int j = 0; j < sheets[s][i].length; j++) {
                            if (sheets[s][i][j] != null) {
                                row.createCell(j).setCellValue(sheets[s][i][j]);
                            }
                        }
                    }
                }
                workbook.write(out);
            }
            return file.getAbsolutePath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
