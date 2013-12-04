package com.jexunit.core.data;

import java.util.Map;

import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

/**
 * Helper-class for generating entities out of the data of the excel-files. In the excel-file the
 * attributes names and values can be set.
 * 
 * @author fabian
 * 
 */
public class TestObjectHelper {

	/**
	 * Create a new instance of the given type with attributes set out of the TestCase. The TestCase
	 * contains the attributes names and values to set.
	 * 
	 * @param testCase
	 *            TestCase, containing the attributes (names) and values to set
	 * @param clazz
	 *            type of the object/instance to create
	 * @return a new instance of the given type with attributes set defined in the TestCase
	 * @throws Exception
	 */
	public static <T> T createObject(TestCase testCase, Class<T> clazz) throws Exception {
		T obj = clazz.newInstance();
		return createObject(testCase, obj);
	}

	/**
	 * Change the values of the given object set in the TestCase. So you can override the objects
	 * values set in a first command with (some) values set in a l
	 * 
	 * @param testCase
	 *            TestCase containing the attributes (names) and values to change
	 * @param object
	 *            object instance to change the attributes values
	 * @return the given object instance with modified attribute-values
	 * @throws Exception
	 */
	public static <T> T createObject(TestCase testCase, T object) throws Exception {
		for (Map.Entry<String, TestCell> entry : testCase.getValues().entrySet()) {
			setPropertyToObject(object, entry.getKey(), entry.getValue().getValue());
		}
		return object;
	}

	/**
	 * Set the attribute/property of the given object to the given value.
	 * 
	 * @param obj
	 *            object/instance
	 * @param propName
	 *            property-name
	 * @param propValue
	 *            property-value
	 * @throws Exception
	 */
	private static void setPropertyToObject(Object obj, String propName, String propValue)
			throws Exception {
		new PropertyUtils().setProperty(obj, propName, propValue);
	}

}
