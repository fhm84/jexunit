package com.jexunit.core.dataprovider;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.model.TestCase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ExcelLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void type() throws Exception {
        assertThat(ExcelLoader.class, notNullValue());
    }

    @Test
    public void shouldReadExcel() throws Exception {
        final ExcelLoader target = new ExcelLoader();
        final Map<String, List<TestCase<ExcelMetadata>>> data = target.readExcel(
                Paths.get("", "src", "test", "resources", "loader-test.xlsx").toAbsolutePath().toString());
        assertEquals(1, data.size());
        final List<TestCase<ExcelMetadata>> testCases = data.get("worksheet1");
        assertNotNull(testCases);
        assertEquals(8, testCases.size());

        checkTestCase(testCases.get(0), "test", 4, "3", "8", "11", "1");
        checkTestCase(testCases.get(1), "test2", 4, "1", "1", "2", "4");
        checkTestCase(testCases.get(2), "test", 2, "A", "B", null, null);

        checkTestCase(testCases.get(3), "test2", 4, "5", "6", "12", "0");
        checkTestCase(testCases.get(4), "test", 4, "5", "5", "13", "27");
        checkTestCase(testCases.get(5), "test", 2, "C", "D", null, null);

        checkTestCase(testCases.get(6), "test3", 4, "12", "42", "33", "0");
        checkTestCase(testCases.get(7), "test3", 4, "10", "20", "30", "40");
    }

    private void checkTestCase(final TestCase<ExcelMetadata> testCase, final String command, final int values,
                               final String val1, final String val2, final String val3, final String val4) {
        assertEquals(command, testCase.getTestCommand());
        assertEquals(values, testCase.getValues().size());
        assertEquals(val1, testCase.getValues().get("val1").getValue());
        assertEquals(val2, testCase.getValues().get("val2").getValue());
        if (val3 == null) {
            assertNull(testCase.getValues().get("val3"));
        } else {
            assertEquals(val3, testCase.getValues().get("val3").getValue());
        }
        if (val4 == null) {
            assertNull(testCase.getValues().get("val4"));
        } else {
            assertEquals(val4, testCase.getValues().get("val4").getValue());
        }
    }

    @Test
    public void shouldReadExcelTransposed() throws Exception {
        final ExcelLoader target = new ExcelLoader(false, true);
        final Map<String, List<TestCase<ExcelMetadata>>> data = target.readExcel(
                Paths.get("", "src", "test", "resources", "loader-test-transpose.xlsx").toAbsolutePath().toString());
        assertEquals(1, data.size());
        final List<TestCase<ExcelMetadata>> testCases = data.get("worksheet1");
        assertNotNull(testCases);
        assertEquals(3, testCases.size());
        assertEquals("test", testCases.get(0).getTestCommand());
        assertEquals(4, testCases.get(0).getValues().size());
    }

    @Test
    public void shouldKeepIntegralNumbersOutsideIntRange() throws Exception {
        try (final XSSFWorkbook workbook = new XSSFWorkbook()) {
            final Row row = workbook.createSheet().createRow(0);
            final ExcelLoader target = new ExcelLoader();

            assertEquals("5", target.cellValues2String(numericCell(row, 0, 5)));
            assertEquals("-7", target.cellValues2String(numericCell(row, 1, -7)));
            assertEquals("3000000000", target.cellValues2String(numericCell(row, 2, 3_000_000_000d)));
            assertEquals("12345678901234", target.cellValues2String(numericCell(row, 3, 12_345_678_901_234d)));
            assertEquals("1.5", target.cellValues2String(numericCell(row, 4, 1.5)));
        }
    }

    @Test
    public void shouldFormatDateTimeCellsWith24HourClock() throws Exception {
        try (final XSSFWorkbook workbook = new XSSFWorkbook()) {
            final CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));
            final Cell cell = workbook.createSheet().createRow(0).createCell(0);
            cell.setCellValue(new GregorianCalendar(2026, Calendar.OCTOBER, 20, 14, 30, 0).getTime());
            cell.setCellStyle(dateTimeStyle);

            assertEquals("20.10.2026 14:30:00", new ExcelLoader().cellValues2String(cell));
        }
    }

    @Test
    public void shouldKeepHeadersAlignedWithValuesOnEmptyHeaderCells() throws Exception {
        final List<TestCase<ExcelMetadata>> testCases = readWorksheet(
                // header line with an empty (not existing) cell in column C
                new String[]{"command", "val1", null, "val3"},
                new String[]{"test", "1", "2", "3"});

        assertEquals(1, testCases.size());
        final TestCase<ExcelMetadata> testCase = testCases.get(0);
        assertEquals("test", testCase.getTestCommand());
        assertEquals("1", testCase.getValues().get("val1").getValue());
        assertEquals("3", testCase.getValues().get("val3").getValue());
        // the value without header is not mapped at all
        assertEquals(2, testCase.getValues().size());
    }

    @Test
    public void shouldMapMultipleCommandsPerLineWithTheirOwnHeaders() throws Exception {
        final List<TestCase<ExcelMetadata>> testCases = readWorksheet(
                new String[]{"command", "val1", "val2", "command", "other"},
                new String[]{"first", "1", "2", "second", "3"});

        assertEquals(2, testCases.size());
        assertEquals("first", testCases.get(0).getTestCommand());
        assertEquals(2, testCases.get(0).getValues().size());
        assertEquals("1", testCases.get(0).getValues().get("val1").getValue());
        assertEquals("2", testCases.get(0).getValues().get("val2").getValue());
        assertEquals("second", testCases.get(1).getTestCommand());
        assertEquals(1, testCases.get(1).getValues().size());
        assertEquals("3", testCases.get(1).getValues().get("other").getValue());
    }

    @Test
    public void shouldMapLinesShorterThanTheHeaderLine() throws Exception {
        final List<TestCase<ExcelMetadata>> testCases = readWorksheet(
                new String[]{"command", "val1", "val2", "command", "other"},
                new String[]{"first", "1"});

        assertEquals(1, testCases.size());
        assertEquals("first", testCases.get(0).getTestCommand());
        assertEquals("1", testCases.get(0).getValues().get("val1").getValue());
    }

    @Test
    public void shouldSkipEmptyCommandsInLine() throws Exception {
        final List<TestCase<ExcelMetadata>> testCases = readWorksheet(
                new String[]{"command", "val1", "command", "other", "command", "last"},
                new String[]{"first", "1", null, "2", "third", "3"});

        assertEquals(2, testCases.size());
        assertEquals("first", testCases.get(0).getTestCommand());
        assertEquals("third", testCases.get(1).getTestCommand());
        assertEquals("3", testCases.get(1).getValues().get("last").getValue());
    }

    @Test
    public void shouldDetectMultilineCommandsWithDefaultCommandPrefix() throws Exception {
        final String prefixKey = JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getKey();
        final String multilineKey = DefaultCommands.MULTILINE_COMMANDS.getConfigKey();
        JExUnitConfig.setConfigProperty(prefixKey, "jx.");
        JExUnitConfig.setConfigProperty(multilineKey, "createPerson, createOrder");
        try {
            final List<TestCase<ExcelMetadata>> testCases = readWorksheet(
                    new String[]{"command", "name"},
                    new String[]{"createPerson", "John"},
                    new String[]{"createPerson", "Jane"},
                    new String[]{"createOrder", "1"},
                    new String[]{"createOrder", "2"});

            assertEquals(2, testCases.size());
            assertTrue(testCases.get(0).isMultiline());
            assertEquals(2, testCases.get(0).getMultilineValues().size());
            assertEquals("Jane", testCases.get(0).getValues().get("name").getValue());
            assertTrue(testCases.get(1).isMultiline());
            assertEquals(2, testCases.get(1).getMultilineValues().size());
        } finally {
            JExUnitConfig.setConfigProperty(prefixKey, JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getDefaultConfig());
            JExUnitConfig.setConfigProperty(multilineKey, DefaultCommands.MULTILINE_COMMANDS.getDefaultValue());
        }
    }

    /**
     * Write the given lines to a worksheet "worksheet1" of a new excel file and read it.
     *
     * @param lines the cell values per line (<code>null</code> for a not existing cell)
     * @return the test cases read from the worksheet
     * @throws Exception in case that something goes wrong
     */
    private List<TestCase<ExcelMetadata>> readWorksheet(final String[]... lines) throws Exception {
        final File excelFile = File.createTempFile("worksheet", ".xlsx", tempFolder.getRoot());
        try (final XSSFWorkbook workbook = new XSSFWorkbook()) {
            final Sheet sheet = workbook.createSheet("worksheet1");
            for (int i = 0; i < lines.length; i++) {
                final Row row = sheet.createRow(i);
                for (int j = 0; j < lines[i].length; j++) {
                    if (lines[i][j] != null) {
                        row.createCell(j).setCellValue(lines[i][j]);
                    }
                }
            }
            try (final OutputStream out = new FileOutputStream(excelFile)) {
                workbook.write(out);
            }
        }
        return new ExcelLoader().readExcel(excelFile.getAbsolutePath()).get("worksheet1");
    }

    private Cell numericCell(final Row row, final int column, final double value) {
        final Cell cell = row.createCell(column);
        cell.setCellValue(value);
        return cell;
    }

}
