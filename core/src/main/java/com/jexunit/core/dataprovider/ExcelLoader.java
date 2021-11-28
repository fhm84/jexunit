package com.jexunit.core.dataprovider;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for reading the excel file into the internal data representation.
 *
 * @author fabian
 */
public class ExcelLoader {

    final boolean worksheetAsTest;
    final boolean transpose;

    public ExcelLoader() {
        this.worksheetAsTest = true;
        this.transpose = false;
    }

    /**
     * @param worksheetAsTest "group" all the test-commands of a worksheet to one test (true) or run each test-command
     *                        as single test (false)
     * @param transpose       transpose data when reading. If set to <code>false</code>, data is read row wise, else if
     *                        set to <code>true</code> data is read column wise
     */
    public ExcelLoader(final boolean worksheetAsTest, final boolean transpose) {
        this.worksheetAsTest = worksheetAsTest;
        this.transpose = transpose;
    }

    /**
     * Load the excel-file and prepare the data (TestCommands). Each worksheet will run as separated Unit-Test.
     *
     * @param excelFile the name of the excel file (to be loaded). It has to be the filename incl. path to be
     *                  loaded (for example: src/test/resources/myExcelFile.xls)
     * @return a list of the parsed {@link TestCase}s
     * @throws Exception in case that something goes wrong
     */
    public Collection<Object[]> loadTestData(final String excelFile) throws Exception {
        final Map<String, List<TestCase<ExcelMetadata>>> tests = readExcel(excelFile);

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
     * @return a map with the excel worksheet name as key and the list of {@link TestCase}s as value
     * @throws Exception in case that something goes wrong
     */
    Map<String, List<TestCase<ExcelMetadata>>> readExcel(final String excelFilePath) throws Exception {
        final Map<String, List<TestCase<ExcelMetadata>>> tests = new LinkedHashMap<>();

        String sheet = null;
        try (final OPCPackage pkg = OPCPackage.open(excelFilePath, PackageAccess.READ)) {
            final XSSFWorkbook workbook = new XSSFWorkbook(pkg);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            // iterate through the worksheets
            for (final Sheet worksheet : workbook) {
                sheet = worksheet.getSheetName();
                final List<TestCase<ExcelMetadata>> testCases = readWorksheet(worksheet);

                tests.put(worksheet.getSheetName(), testCases);
            }
        } catch (final FileNotFoundException e) {
            throw new Exception(String.format("Excel-file '%s' not found!", excelFilePath), e);
        } catch (final Exception e) {
            throw new Exception(String.format("Error while reading the excel-file! - worksheet: %s", sheet), e);
        }
        return tests;
    }

    private List<TestCase<ExcelMetadata>> readWorksheet(final Sheet worksheet) throws Exception {
        final List<List<Cell>> cells = new ArrayList<>();

        if (transpose) {
            final Pair<Integer, Integer> lastRowColumn = getLastRowAndLastColumn(worksheet);
            final int lastRow = lastRowColumn.getLeft();
            final int lastColumn = lastRowColumn.getRight();

            for (int rowNum = 0; rowNum <= lastRow; rowNum++) {
                final Row row = worksheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                for (int columnNum = 0; columnNum < lastColumn; columnNum++) {
                    if (cells.size() <= columnNum) {
                        cells.add(new LinkedList<>());
                    }
                    final Cell cell = row.getCell(columnNum);
                    cells.get(columnNum).add(cell);
                }
            }
        } else {
            // iterate through the rows
            for (int i = 0; i <= worksheet.getLastRowNum(); i++) {
                final Row row = worksheet.getRow(i);

                if (row != null) {
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        while (cells.size() <= i) {
                            cells.add(new LinkedList<>());
                        }
                        final List<Cell> list = cells.get(i);
                        final Cell cell = row.getCell(j);
                        list.add(cell);
                    }
                }
            }
        }
        return mapCells(cells);
    }

    private Pair<Integer, Integer> getLastRowAndLastColumn(final Sheet sheet) {
        final int lastRow = sheet.getLastRowNum();
        int lastColumn = 0;
        for (final Row row : sheet) {
            lastColumn = Math.max(lastColumn, row.getLastCellNum());
        }
        return new ImmutablePair<>(lastRow, lastColumn);
    }

    /**
     * Map given cells to test cases.
     *
     * @param cells cells (read from excel worksheet - independent if transposed or not)
     * @return list of test cases to be executed
     * @throws Exception
     */
    private List<TestCase<ExcelMetadata>> mapCells(final List<List<Cell>> cells) throws Exception {
        final List<TestCase<ExcelMetadata>> testCases = new ArrayList<>();

        if (cells == null || cells.isEmpty()) {
            return testCases;
        }

        List<String> commandHeaders = null;

        // this is always the current cell (use for detailed exception message in case of an exception)
        Cell cell = null;
        // marker for "newly defined" command line (to e.g. reset the multiline flag
        boolean commandLine = false;
        try {
            for (final List<Cell> cellList : cells) {
                if (cellList.isEmpty()) {
                    continue;
                }
                cell = cellList.get(0);
                final String cellValue = cellValues2String(cell);
                if (cellValue == null || cellValue.isEmpty()) {
                    // if the first column is empty, this is a comment line and will be ignored
                    continue;
                } else if (JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_STATEMENT)
                        .equalsIgnoreCase(cellValue)) {
                    commandHeaders = new ArrayList<>();
                    commandLine = true;

                    // iterate through following cells
                    for (int h = 0; h < cellList.size(); h++) {
                        cell = cellList.get(h);
                        if (cell != null) {
                            commandHeaders.add(cell.getStringCellValue());
                        }
                    }
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED)
                        .equalsIgnoreCase(cellValue)) {
                    final TestCase<ExcelMetadata> testCase = new TestCase<>(new ExcelMetadata());

                    // the first column is always the command
                    testCase.setTestCommand(cellValue);
                    testCase.getMetadata().setSheet(cell.getSheet().getSheetName());
                    testCase.getMetadata().setIdentifier(cell.getAddress().formatAsString());

                    if (cellList.size() >= 1) {
                        final TestCell testCell = new TestCell();
                        cell = cellList.get(1);
                        testCell.setValue(cellValues2String(cell));
                        testCell.setIdentifier(cell.getAddress().formatAsString());
                        testCase.getValues().put(
                                JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED),
                                testCell);
                        testCase.setDisabled(Boolean.parseBoolean(testCell.getValue()));
                    }
                    testCases.add(testCase);
                    commandLine = false;
                } else if (commandHeaders != null || JExUnitConfig
                        .getDefaultCommandProperty(DefaultCommands.REPORT).equalsIgnoreCase(cellValue)) {
                    final TestCase<ExcelMetadata> lastTestCase =
                            testCases.isEmpty() ? null : testCases.get(testCases.size() - 1);

                    if (!commandLine && lastTestCase != null && lastTestCase.isMultiline()
                            && cellValue.equalsIgnoreCase(lastTestCase.getTestCommand())) {
                        lastTestCase.next();

                        map(commandHeaders, cellList.subList(0, cellList.size()), lastTestCase);
                    } else {
                        testCases.addAll(mapTestCases(cellList, commandHeaders));
                    }

                    commandLine = false;
                }
            }
        } catch (final Exception e) {
            if (cell != null) {
                throw new Exception(String.format("Error while reading the excel-file! - worksheet: %s address: %s",
                        cell.getSheet().getSheetName(), cell.getAddress().formatAsString()), e);
            } else {
                throw e;
            }
        }

        return testCases;
    }

    private List<TestCase<ExcelMetadata>> mapTestCases(final List<Cell> cellList, final List<String> commandHeaders) {
        final List<TestCase<ExcelMetadata>> testCases = new ArrayList<>();
        if (commandHeaders == null || commandHeaders.isEmpty()) {
            return testCases;
        }

        int nextCommandIndex = 0;
        int lastCommandIndex;
        do {
            lastCommandIndex = nextCommandIndex;
            final Cell commandCell = cellList.get(lastCommandIndex);
            nextCommandIndex = getNextCommandIndex(commandHeaders, nextCommandIndex);

            final TestCase<ExcelMetadata> testCase = new TestCase<>(new ExcelMetadata());
            testCase.setTestCommand(cellValues2String(commandCell));
            testCase.getMetadata().setSheet(commandCell.getSheet().getSheetName());
            testCase.getMetadata().setIdentifier(commandCell.getAddress().formatAsString());

            final List<Cell> commandValueCells =
                    cellList.subList(lastCommandIndex, nextCommandIndex > -1 ? nextCommandIndex : cellList.size());
            map(commandHeaders, commandValueCells, testCase);

            testCases.add(testCase);
        } while (nextCommandIndex != -1 && nextCommandIndex < cellList.size());

        return testCases;
    }

    private int getNextCommandIndex(final List<String> commandHeaders, final int lastIndex) {
        for (int i = lastIndex + 1; i < commandHeaders.size(); i++) {
            if (JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_STATEMENT)
                    .equalsIgnoreCase(commandHeaders.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void map(final List<String> commandHeaders, final List<Cell> cells,
                     final TestCase<ExcelMetadata> testCase) {
        for (int j = 0; j < cells.size(); j++) {
            final Cell cell = cells.get(j);
            if (cell == null) {
                continue;
            }
            final TestCell testCell = new TestCell();
            testCell.setValue(cellValues2String(cell));
            testCell.setIdentifier(cell.getAddress().formatAsString());
            // the "report"-command doesn't need a header-line
            final String key = commandHeaders != null && commandHeaders.size() > j
                    ? commandHeaders.get(j) : "param" + j;
            if (JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_STATEMENT)
                    .equalsIgnoreCase(key)) {
                testCase.setTestCommand(testCell.getValue());
            } else {
                testCase.getValues().put(key, testCell);
            }

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
                    if (!testCase.isMultiline()) {
                        testCase.setMultiline(Boolean.parseBoolean(testCell.getValue()));
                    }
                    testCase.getValues().remove(key);
                }
            }
        }

        if (testCase.getMultiline() == null) {
            final String[] commands =
                    JExUnitConfig.getDefaultCommandProperty(DefaultCommands.MULTILINE_COMMANDS).split(",");
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
     * @param cell cell (excel)
     * @return the value of the excel-cell as String
     */
    String cellValues2String(final Cell cell) {
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
                        return new SimpleDateFormat(
                                JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATETIME_PATTERN))
                                .format(value);
                    } else {
                        return new SimpleDateFormat(
                                JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
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

}
