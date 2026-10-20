package com.jexunit.core.data;

import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests for the string to object conversion (using the default configuration: date pattern "dd.MM.yyyy" and date-time
 * pattern "dd.MM.yyyy HH:mm:ss").
 */
public class PropertyUtilsTest {

    @Test
    public void shouldConvertIntegralValues() throws Exception {
        assertEquals(5, PropertyUtils.convertPropertyStringToObject(int.class, "5"));
        assertEquals(5, PropertyUtils.convertPropertyStringToObject(Integer.class, "5.0"));
        assertEquals(1000, PropertyUtils.convertPropertyStringToObject(int.class, "1e3"));
        assertEquals(3_000_000_000L, PropertyUtils.convertPropertyStringToObject(long.class, "3000000000"));
        // not exactly representable as double
        assertEquals(9_007_199_254_740_993L,
                PropertyUtils.convertPropertyStringToObject(Long.class, "9007199254740993"));
    }

    @Test
    public void shouldRejectIntegralValuesWithFraction() {
        assertThrows(NumberFormatException.class, () -> PropertyUtils.convertPropertyStringToObject(int.class, "1.9"));
        assertThrows(NumberFormatException.class, () -> PropertyUtils.convertPropertyStringToObject(long.class, "1.9"));
    }

    @Test
    public void shouldRejectIntegralValuesOutOfRange() {
        assertThrows(NumberFormatException.class,
                () -> PropertyUtils.convertPropertyStringToObject(int.class, "3000000000"));
    }

    @Test
    public void shouldConvertDateValues() throws Exception {
        assertEquals(date(2026, Calendar.OCTOBER, 20, 0, 0, 0),
                PropertyUtils.convertPropertyStringToObject(Date.class, "20.10.2026"));
    }

    @Test
    public void shouldKeepTimeOfDateTimeValues() throws Exception {
        assertEquals(date(2026, Calendar.OCTOBER, 20, 14, 30, 15),
                PropertyUtils.convertPropertyStringToObject(Date.class, "20.10.2026 14:30:15"));
    }

    @Test
    public void shouldRejectDateValuesWithTrailingText() {
        assertThrows(ParseException.class,
                () -> PropertyUtils.convertPropertyStringToObject(Date.class, "20.10.2026 tomorrow"));
    }

    private static Date date(final int year, final int month, final int day, final int hour, final int minute,
                             final int second) {
        return new GregorianCalendar(year, month, day, hour, minute, second).getTime();
    }

}
