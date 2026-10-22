package com.jexunit.examples.features;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JExUnit.class)
public class FormattingTest {

    @ExcelFile(worksheetAsTest = false)
    static String[] excelFiles = new String[]{"src/test/resources/FormattingTest.xlsx"};

    @TestCommand("map")
    public static void runMap(final TestCase<?> testCase, final HashMap<String, String> params) {
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
    public static void run(final HashMap<String, String> params) throws ParseException {
        // NOW() and TODAY() are evaluated when the excel file is read, which is some seconds before the command runs,
        // so the timestamp is compared with a tolerance (comparing the formatted values fails on a minute boundary)
        final String timestamp = params.get("timestamp");
        final Date excelNow = new SimpleDateFormat(
                JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATETIME_PATTERN)).parse(timestamp);
        final long age = new Date().getTime() - excelNow.getTime();
        // the pattern has no milliseconds, so the parsed timestamp can be up to a second later than "now"
        assertTrue("NOW() is not the current time: " + timestamp,
                age > -TimeUnit.SECONDS.toMillis(1) && age < TimeUnit.MINUTES.toMillis(5));

        // TODAY() is the date of NOW() (not of the current time, which may be after midnight already), preserving the
        // configured date pattern
        assertEquals(new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
                .format(excelNow), params.get("date"));
    }

    @TestCommand("COMPAREINT")
    public static void compareInt(final HashMap<String, String> params) {
        final String integer = params.get("integer");
        assertEquals("1", integer);

        final String aFloat = params.get("float");
        assertEquals("1.999", aFloat);
    }

}


