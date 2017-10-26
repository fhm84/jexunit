package com.jexunit.core.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ognl.DefaultTypeConverter;

/**
 * Cutstom type converter extending the default type converter of ognl for handling integer and long values, because
 * JExUnit will extract these kinds of variables as double and the default type converter cannot convert double values
 * to integer or long values.
 *
 * @author fabian
 *
 */
public class CustomTypeConverter extends DefaultTypeConverter {

	private static Logger log = Logger.getLogger(CustomTypeConverter.class.getName());

	/*
	 * (non-Javadoc)
	 *
	 * @see ognl.TypeConverter#convertValue(java.util.Map, java.lang.Object, java.lang.reflect.Member, java.lang.String,
	 * java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object convertValue(Map context, Object target, Member member, String propertyName, Object value,
			Class toType) {
		if (value == null) {
			return null;
		}

		if (!(value instanceof String)) {
			return super.convertValue(context, target, member, propertyName, value, toType);
		}

		try {
			String stringValue = (String) value;
			return PropertyUtils.convertPropertyStringToObject(toType, stringValue);
		} catch (ParseException | IllegalArgumentException | SecurityException |
                IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { toType, value });
		}

		return super.convertValue(context, target, member, propertyName, value, toType);
	}

}
