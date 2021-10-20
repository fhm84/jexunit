package com.jexunit.core.dataprovider;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for reading the excel file into the internal data representation.
 * <p>
 * TODO: add possibility to change the "test-direction" from horizontal to vertical?
 *
 * @author fabian
 */
public class ExcelLoader {

    // Utility class, only static access
    private ExcelLoader() {
    }

    /**
     * Load the excel-file and prepare the data (TestCommands). Each worksheet will run as separated Unit-Test.
     *
     * @param excelFile       the name of the excel file (to be loaded). It has to be the filename incl. path to be
     *                        loaded (for example: src/test/resources/myExcelFile.xls)
     * @param worksheetAsTest "group" all the test-commands of a worksheet to one test (true) or run each test-command
     *                        as single test (false)
     * @param transpose       transpose data when reading. If set to <code>false</code>, data is read row wise, else if
     *                        set to <code>true</code> data is read column wise
     * @return a list of the parsed {@link TestCase}s
     * @throws Exception in case that something goes wrong
     */
    public static Collection<Object[]> loadTestData(final String excelFile, final boolean worksheetAsTest,
                                                    final boolean transpose) throws Exception {
        final Map<String, List<TestCase<?>>> tests = readExcel(excelFile, transpose);

        final Collection<Object[]> col = new ArrayList<>();
        if (worksheetAsTest) {
            tests.forEach((k, v) -> col.add(new Object[]{v}));
        } else {
            tests.forEach((s, l) -> l.forEach(gtc -> {
                final List<TestCase<?>> list = new ArrayList<>();
                list.add(gtc);
                col.add(new Object[]{list});
            }));
        }

        return col;
    }

    /**
     * Read the excel-sheet and generate the TestCases. Each worksheet will become its own list of TestCases. So
     * each worksheet will run as separated test run.
     *
     * @param excelFilePath the path to the excel-file to read
     * @param transpose     transpose data when reading. If set to <code>false</code>, data is read row wise, else if
     *                      set to <code>true</code> data is read column wise
     * @return a map with the excel worksheet name as key and the list of {@link TestCase}s as value
     * @throws Exception in case that something goes wrong
     */
    static Map<String, List<TestCase<?>>> readExcel(final String excelFilePath,
                                                    final boolean transpose) throws Exception {
        final Map<String, List<TestCase<?>>> tests = new LinkedHashMap<>();

        int i = 0;
        final int j = 0;
        String sheet = null;
        try (final OPCPackage pkg = OPCPackage.open(excelFilePath, PackageAccess.READ)) {
            final XSSFWorkbook workbook = new XSSFWorkbook(pkg);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            // iterate through the worksheets
            for (final Sheet worksheet : workbook) {
                sheet = worksheet.getSheetName();
                final List<TestCase<?>> testCases = new ArrayList<>();

                List<String> commandHeaders = null;

                // iterate through the rows
                for (i = 0; i <= worksheet.getLastRowNum(); i++) {
                    final Row row = worksheet.getRow(i);

                    if (row != null) {
                        final String cellValue = cellValues2String(workbook, row.getCell(0));
                        if (JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_STATEMENT)
                                .equalsIgnoreCase(cellValue)) {
                            commandHeaders = new ArrayList<>();

                            // iterate through the columns
                            for (int h = 0; h < row.getLastCellNum(); h++) {
                                if (row.getCell(h) != null) {
                                    commandHeaders.add(row.getCell(h).getStringCellValue());
                                }
                            }
                        } else {
                            if (cellValue == null || cellValue.isEmpty()) {
                                // if the first column is empty, this is a comment line and will be
                                // ignored
                                continue;
                            } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED)
                                    .equalsIgnoreCase(cellValue)) {
                                final TestCase<ExcelMetadata> testCase = new TestCase<>(new ExcelMetadata());

                                // the first column is always the command
                                testCase.setTestCommand(cellValue);
                                testCase.getMetadata().setSheet(worksheet.getSheetName());
                                testCase.getMetadata().setRow(row.getRowNum() + 1);

                                if (row.getLastCellNum() >= 1) {
                                    final TestCell testCell = new TestCell();
                                    testCell.setValue(cellValues2String(workbook, row.getCell(1)));
                                    testCell.setColumn(row.getCell(1).getColumnIndex() + 1);
                                    testCase.getValues().put(
                                            JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED),
                                            testCell);
                                    testCase.setDisabled(Boolean.parseBoolean(testCell.getValue()));
                                }
                                testCases.add(testCase);
                            } else if (commandHeaders != null || JExUnitConfig
                                    .getDefaultCommandProperty(DefaultCommands.REPORT).equalsIgnoreCase(cellValue)) {
                                final TestCase<ExcelMetadata> testCase = new TestCase<>(new ExcelMetadata());

                                // the first column is always the command
                                testCase.setTestCommand(cellValue);
                                testCase.getMetadata().setSheet(worksheet.getSheetName());
                                testCase.getMetadata().setRow(row.getRowNum() + 1);

                                fillvaluesinmap(workbook, commandHeaders, row, testCase);

                                if (testCase.isMultiline()) {
                                    while (worksheet.getRow(i + 1) != null &&
                                            cellValue.equalsIgnoreCase(cellValues2String(workbook,
                                                    worksheet.getRow(i + 1).getCell(0)))) {
                                        i++;
                                        testCase.next();
                                        fillvaluesinmap(workbook, commandHeaders, worksheet.getRow(i), testCase);
                                    }
                                }

                                testCases.add(testCase);
                            }
                        }
                    }
                }

                tests.put(worksheet.getSheetName(), testCases);
            }
        } catch (final FileNotFoundException e) {
            throw new Exception(String.format("Excel-file '%s' not found!", excelFilePath), e);
        } catch (final Exception e) {
            throw new Exception(String.format("Error while reading the excel-file! - worksheet: %s row: %s column: %s",
                    sheet, i + 1, getColumn(j + 1)), e);
        }
        return tests;
    }

    private static void fillvaluesinmap(final XSSFWorkbook workbook, final List<String> commandHeaders, final Row row,
                                        final TestCase<ExcelMetadata> testCase) {
        for (int j = 1; j < row.getLastCellNum(); j++) {
            if (row.getCell(j) == null) {
                continue;
            }
            final TestCell testCell = new TestCell();
            testCell.setValue(cellValues2String(workbook, row.getCell(j)));
            testCell.setColumn(row.getCell(j).getColumnIndex() + 1);
            // the "report"-command doesn't need a header-line
            final String key = commandHeaders != null && commandHeaders.size() > j
                    ? commandHeaders.get(j) : "param" + j;
            testCase.getValues().put(key, testCell);

            // read/parse the "default" commands/parameters
            if (commandHeaders != null && commandHeaders.size() > j) {
                final String header = commandHeaders.get(j);
                if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.BREAKPOINT)
                        .equalsIgnoreCase(commandHeaders.get(j))) {
                    // each command has the ability to set a breakpoint to
                    // debug the test more easily
                    testCase.setBreakpointEnabled(Boolean.parseBoolean(testCell.getValue()));
                } else if (JExUnitConfig
                        .getDefaultCommandProperty(DefaultCommands.EXCEPTION_EXPECTED)
                        .equalsIgnoreCase(header)) {
                    // each command has the ability to expect an exception.
                    // you can define this via the field EXCEPTION_EXPECTED.
                    testCase.setExceptionExpected(Boolean.parseBoolean(testCell.getValue()));
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED)
                        .equalsIgnoreCase(header)) {
                    // each command can be disabled
                    testCase.setDisabled(Boolean.parseBoolean(testCell.getValue()));
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.COMMENT)
                        .equalsIgnoreCase(header)) {
                    // add the comment to the test-case
                    testCase.setComment(testCell.getValue());
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.FAST_FAIL)
                        .equalsIgnoreCase(header)) {
                    // the command can fast fail the complete test sheet on fail
                    testCase.setFastFail(Boolean.parseBoolean(testCell.getValue()));
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.MULTILINE)
                        .equalsIgnoreCase(header)) {
                    // the command can fast fail the complete test sheet on fail
                    testCase.setMultiline(Boolean.parseBoolean(testCell.getValue()));
                    testCase.getValues().remove(key);
                }
            }
        }

        if (testCase.getMultiline() == null) {
            final String[] commands = JExUnitConfig.getDefaultCommandProperty(DefaultCommands.MULTILINE_COMMANDS).split(",");
            for (final String command : commands) {
                if (command.equalsIgnoreCase(testCase.getTestCommand())) {
                    testCase.setMultiline(true);
                }
            }
        }

    }

    /**
     * Get the value of the excel-cell as String.
     *
     * @param workbook workbook (excel) for evaluating cell formulas
     * @param cell     cell (excel)
     * @return the value of the excel-cell as String
     */
    static String cellValues2String(final XSSFWorkbook workbook, final Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    final Date value = cell.getDateCellValue();
                    // Test if date is datetime. Does format contain letter h?
                    if (cell.getCellStyle().getDataFormatString() != null &&
                            cell.getCellStyle().getDataFormatString().toLowerCase().contains("h")) {
                        return new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATETIME_PATTERN))
                                .format(value);
                    } else {
                        return new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
                                .format(value);
                    }
                } else {
                    final double number = cell.getNumericCellValue();
                    if ((number == Math.floor(number)) && !Double.isInfinite(number)) {
                        return String.valueOf(new Double(number).intValue());
                    }
                    return String.valueOf(number);
                }
            case STRING:
                return cell.getStringCellValue();
            case BLANK:
                return cell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case ERROR:
                return String.valueOf(cell.getErrorCellValue());
        }
        return null;
    }

    /**
     * Get the character(s) of the column like it is in excel (A, B, C, ...)
     *
     * @param column the column index
     * @return the name of the column (A, B, C, ...)
     */
    public static String getColumn(int column) {
        column--;
        if (column >= 0 && column < 26) {
            return Character.toString((char) ('A' + column));
        } else if (column > 25) {
            return getColumn(column / 26) + getColumn(column % 26 + 1);
        } else {
            throw new IllegalArgumentException("Invalid Column #" + Character.toString((char) (column + 1)));
        }
    }

}
