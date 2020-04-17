package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
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
                return Double.valueOf(value).intValue();
            } else if (clazz == Double.class || clazz == double.class) {
                return Double.parseDouble(value);
            } else if (clazz == Long.class || clazz == long.class) {
                return Double.valueOf(value).longValue();
            } else if (clazz == Float.class || clazz == float.class) {
                return Float.parseFloat(value);
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (clazz == BigDecimal.class) {
                return new BigDecimal(value);
            } else if (clazz == Date.class) {
                // TODO: Add possibility to parse Timestamps
                return new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
                        .parse(value);
            } else if (clazz.isEnum()) {
                return clazz.getMethod("valueOf", String.class).invoke(clazz, value);
            }
            return value;
        } catch (final ParseException | IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
            log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[]{clazz, value});
            throw e;
        }
    }

}
