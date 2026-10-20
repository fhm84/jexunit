package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will provide useful utility methods for properties handling.
 *
 * @author fabian
 */
class PropertyUtils {

    private static final Logger log = Logger.getLogger(PropertyUtils.class.getName());

    /**
     * Patterns to parse dates with, in order. The date-time pattern comes first, as a date-only value cannot match it.
     */
    private static final JExUnitConfig.ConfigKey[] DATE_PATTERNS = {
            JExUnitConfig.ConfigKey.DATETIME_PATTERN, JExUnitConfig.ConfigKey.DATE_PATTERN};

    /**
     * Convert the given (property-)value (a string) to the given type.
     *
     * @param clazz the type of the property (to convert the property to)
     * @param value the property-value (as string)
     * @return the property-value (converted to the expected type)
     * @throws ParseException            if the value cannot be converted
     * @throws IllegalAccessException    if the value cannot be converted to an enum
     * @throws InvocationTargetException if the value cannot be converted to an enum
     * @throws NoSuchMethodException     if the value cannot be converted to an enum
     */
    public static Object convertPropertyStringToObject(final Class<?> clazz, final String value)
            throws ParseException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (value == null) {
            return null;
        }
        try {
            if (clazz == Integer.class || clazz == int.class) {
                return toIntegral(clazz, value);
            } else if (clazz == Double.class || clazz == double.class) {
                return Double.parseDouble(value);
            } else if (clazz == Long.class || clazz == long.class) {
                return toIntegral(clazz, value);
            } else if (clazz == Float.class || clazz == float.class) {
                return Float.parseFloat(value);
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (clazz == BigDecimal.class) {
                return new BigDecimal(value);
            } else if (clazz == Date.class) {
                // TODO: Add possibility to parse Timestamps
                return toDate(value);
            } else if (clazz.isEnum()) {
                return clazz.getMethod("valueOf", String.class).invoke(clazz, value);
            }
            return value;
        } catch (final ParseException | IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
            log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[]{clazz, value});
            throw e;
        }
    }

    /**
     * Convert the given value to an int or long. Integral values with a zero fraction or in exponent notation (like
     * "5.0" or "1e3") are accepted, but values with a fraction or out of range are rejected instead of being truncated.
     *
     * @param clazz the integral type to convert to (int/Integer or long/Long)
     * @param value the property-value (as string)
     * @return the value as Integer or Long
     * @throws NumberFormatException if the value is not a valid integral value of the given type
     */
    private static Number toIntegral(final Class<?> clazz, final String value) {
        final BigDecimal number = new BigDecimal(value.trim());
        try {
            if (clazz == Long.class || clazz == long.class) {
                return number.longValueExact();
            }
            return number.intValueExact();
        } catch (final ArithmeticException e) {
            throw new NumberFormatException(String.format("'%s' is not a valid %s value", value, clazz.getName()));
        }
    }

    /**
     * Convert the given value to a date. The value has to match the configured date-time pattern or the configured
     * date pattern completely (ignoring surrounding whitespace), so the time of a date-time value is not dropped
     * silently.
     *
     * @param value the property-value (as string)
     * @return the parsed date
     * @throws ParseException if the value matches none of the configured patterns
     */
    private static Date toDate(final String value) throws ParseException {
        final String trimmed = value.trim();
        for (final JExUnitConfig.ConfigKey key : DATE_PATTERNS) {
            final ParsePosition position = new ParsePosition(0);
            final Date date = new SimpleDateFormat(JExUnitConfig.getStringProperty(key)).parse(trimmed, position);
            if (date != null && position.getIndex() == trimmed.length()) {
                return date;
            }
        }
        throw new ParseException(String.format("Unparseable date: '%s'", value), 0);
    }

}
