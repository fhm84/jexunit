package com.jexunit.examples.features;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JExUnit.class)
public class FormattingTest {

    @ExcelFile(worksheetAsTest = false)
    static String[] excelFiles = new String[]{"src/test/resources/FormattingTest.xlsx"};

    @TestCommand("map")
    public static void runMap(final TestCase<?> testCase, final HashMap<String, String> params) throws Exception {
        final String param1 = params.get("param1");
        final String param2 = params.get("param2");

        // Random Int generator is evaluated exactly once
        assertEquals(param1, param2);

        // Map key without value is truncated
        assertFalse(params.containsKey("param3"));

        // param3 as well as empty date formatted cell in G6 is ignored
        assertEquals(2, params.size());
    }

    @TestCommand("COMPAREDATE")
    public static void run(final HashMap<String, String> params) {
        String date = params.get("date");
        // Same date than today preserving configured date pattern
        String format = new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
                .format(new Date());

        assertEquals(format, date);

        date = params.get("timestamp");
        // Same date than today preserving configured date pattern
        format = new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATETIME_PATTERN))
                .format(new Date());
        // Compare two timestamps (From Excel and Java). Because there is a little time-difference (maximum 1 second) between these dates
        assertEquals(format.substring(0, format.length() - 2), date.substring(0, date.length() - 2));
    }

    @TestCommand("COMPAREINT")
    public static void compareInt(final HashMap<String, String> params) {
        final String integer = params.get("integer");
        assertEquals("1", integer);

        final String aFloat = params.get("float");
        assertEquals("1.999", aFloat);
    }
}


