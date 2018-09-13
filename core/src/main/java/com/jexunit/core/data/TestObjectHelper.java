package com.jexunit.core.data;

import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Map;

/**
 * Helper-class for generating entities out of the data of the excel-files. In the excel-file the attributes names and
 * values can be set.
 *
 * @author fabian
 */
public class TestObjectHelper {

    /**
     * Create a new instance of the given type with attributes set out of the TestCase. The TestCase contains the
     * attributes names and values to set.
     *
     * @param testCase TestCase, containing the attributes (names) and values to set
     * @param clazz    type of the object/instance to create
     * @param <T>      generic type
     * @return a new instance of the given type with attributes set defined in the TestCase
     * @throws Exception in case that something goes wrong
     */
    public static <T> T createObject(final TestCase<?> testCase, final Class<T> clazz) throws Exception {
        final T obj = clazz.newInstance();
        return createObject(testCase, obj);
    }

    /**
     * Change the values of the given object set in the TestCase. So you can override the objects values set in a first
     * command with (some) values set in a l
     *
     * @param testCase TestCase containing the attributes (names) and values to change
     * @param object   object instance to change the attributes values
     * @param <T>      generic type
     * @return the given object instance with modified attribute-values
     * @throws Exception in case that something goes wrong
     */
    public static <T> T createObject(final TestCase<?> testCase, final T object) throws Exception {
        for (final Map.Entry<String, TestCell> entry : testCase.getValues().entrySet()) {
            OgnlUtils.setPropertyToObject(object, entry.getKey(), entry.getValue().getValue());
        }
        return object;
    }

    public static Object getProperty(final Object object, final String propertyKey) throws Exception {
        return OgnlUtils.getProperty(object, propertyKey);
    }

    /**
     * Get the property identified by the given key out of the test-case.
     *
     * @param testCase    the test-case to get the property from
     * @param propertyKey the identifier for the property (like defined in the excel-file)
     * @return the property identified by the given propertyKey if found, else null
     */
    public static String getPropertyByKey(final TestCase<?> testCase, final String propertyKey) {
        for (final Map.Entry<String, TestCell> entry : testCase.getValues().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(propertyKey)) {
                return entry.getValue().getValue();
            }
        }
        return null;
    }

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
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ParseException {
        return PropertyUtils.convertPropertyStringToObject(clazz, value);
    }

}
