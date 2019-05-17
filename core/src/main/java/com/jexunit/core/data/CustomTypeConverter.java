package com.jexunit.core.data;

import ognl.DefaultTypeConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cutstom type converter extending the default type converter of ognl for handling integer and long values, because
 * JExUnit will extract these kinds of variables as double and the default type converter cannot convert double values
 * to integer or long values.
 *
 * @author fabian
 */
public class CustomTypeConverter extends DefaultTypeConverter {

    private static final Logger log = Logger.getLogger(CustomTypeConverter.class.getName());

    /*
     * (non-Javadoc)
     *
     * @see ognl.TypeConverter#convertValue(java.util.Map, java.lang.Object, java.lang.reflect.Member, java.lang.String,
     * java.lang.Object, java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object convertValue(final Map context, final Object target, final Member member, final String propertyName,
                               final Object value, final Class toType) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof String)) {
            return super.convertValue(context, target, member, propertyName, value, toType);
        }

        try {
            final String stringValue = (String) value;
            return PropertyUtils.convertPropertyStringToObject(toType, stringValue);
        } catch (final ParseException | IllegalArgumentException | SecurityException |
                IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[]{toType, value});
        }

        return super.convertValue(context, target, member, propertyName, value, toType);
    }

}
