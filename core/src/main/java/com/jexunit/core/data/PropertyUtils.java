package com.jexunit.core.data;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jexunit.core.JExUnitConfig;

/**
 * This class will provide useful utility methods for properties handling.
 * 
 * @author fabian
 * 
 */
class PropertyUtils {

	private static Logger log = Logger.getLogger(PropertyUtils.class.getName());

	/**
	 * Convert the given (property-)value (a string) to the given type.
	 * 
	 * @param clazz
	 *            the type of the property (to convert the property to)
	 * @param value
	 *            the property-value (as string)
	 * @return the property-value (converted to the expected type)
	 * @throws ParseException
	 *             if the value cannot be converted
	 * @throws IllegalAccessException
	 *             if the value cannot be converted to an enum
	 * @throws InvocationTargetException
	 *             if the value cannot be converted to an enum
	 * @throws NoSuchMethodException
	 *             if the value cannot be converted to an enum
	 */
	public static Object convertPropertyStringToObject(Class<?> clazz, String value)
			throws ParseException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (value == null) {
			return null;
		}
		try {
			if (clazz == Integer.class || clazz == int.class) {
				Double d = Double.parseDouble(value);
				if (d != null) {
					return d.intValue();
				}
			} else if (clazz == Double.class || clazz == double.class) {
				return Double.parseDouble(value);
			} else if (clazz == Long.class || clazz == long.class) {
				Double d = Double.parseDouble(value);
				if (d != null) {
					return d.longValue();
				}
			} else if (clazz == Float.class || clazz == float.class) {
				return Float.parseFloat(value);
			} else if (clazz == Boolean.class || clazz == boolean.class) {
				return Boolean.parseBoolean(value);
			} else if (clazz == BigDecimal.class) {
				return new BigDecimal(value);
			} else if (clazz == Date.class) {
				return new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
						.parse(value);
			} else if (clazz.isEnum()) {
				return clazz.getMethod("valueOf", String.class).invoke(clazz, value);
			}
			return value;
		} catch (NumberFormatException | ParseException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz, value });
			throw e;
		} catch (IllegalArgumentException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz, value });
			throw e;
		} catch (SecurityException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz, value });
			throw e;
		}
	}
}
