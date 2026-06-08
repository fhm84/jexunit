package com.jexunit.core.dataprovider;

import com.jexunit.core.model.TestCase;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ExcelLoaderTest {

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

}
