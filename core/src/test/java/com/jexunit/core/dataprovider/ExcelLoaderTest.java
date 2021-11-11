package com.jexunit.core.dataprovider;

import com.jexunit.core.model.TestCase;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExcelLoaderTest {

    @Test
    public void type() throws Exception {
        assertThat(ExcelLoader.class, notNullValue());
    }

    @Test
    public void getColumn_A$int() throws Exception {
        final ExcelLoader target = new ExcelLoader();

        int column = 1;
        String actual = target.getColumn(column);
        String expected = "A";
        assertThat(actual, is(equalTo(expected)));

        column = 26;
        expected = "Z";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));

        column = 28;
        expected = "AB";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));

        column = 52;
        expected = "AZ";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));

        column = 60;
        expected = "BH";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));

        column = 705;
        expected = "AAC";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));

        column = 4136;
        expected = "FCB";
        actual = target.getColumn(column);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldReadExcel() throws Exception {
        final ExcelLoader target = new ExcelLoader();
        final Map<String, List<TestCase<ExcelMetadata>>> data = target.readExcel(Paths.get("", "src", "test", "resources", "loader-test.xlsx").toAbsolutePath().toString());
        assertEquals(1, data.size());
        final List<TestCase<ExcelMetadata>> testCases = data.get("worksheet1");
        assertNotNull(testCases);
        assertEquals(3, testCases.size());
        assertEquals("test", testCases.get(0).getTestCommand());
        assertEquals(4, testCases.get(0).getValues().size());
    }

    @Test
    public void shouldReadExcelTransposed() throws Exception {
        final ExcelLoader target = new ExcelLoader(false, true);
        final Map<String, List<TestCase<ExcelMetadata>>> data = target.readExcel(Paths.get("", "src", "test", "resources", "loader-test-transpose.xlsx").toAbsolutePath().toString());
        assertEquals(1, data.size());
        final List<TestCase<ExcelMetadata>> testCases = data.get("worksheet1");
        assertNotNull(testCases);
        assertEquals(3, testCases.size());
        assertEquals("test", testCases.get(0).getTestCommand());
        assertEquals(4, testCases.get(0).getValues().size());
    }

}
