package com.jexunit.core.data;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

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

	private static Logger log = Logger.getLogger(TestObjectHelper.class.getName());

	private static final String PROP_DEL = ".";

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

		for (Map.Entry<String, TestCell> entry : testCase.getValues().entrySet()) {
			setPropertyToObject(obj, entry.getKey(), entry.getValue().getValue());
		}
		return obj;
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
		int idx = propName.indexOf(PROP_DEL);

		if (idx != -1) {
			String propObj = propName.substring(0, idx);
			Object subObj = getSubObject(obj, propObj);
			setPropertyToObject(subObj, propName.substring(idx + 1), propValue);
		} else {
			Class<?> clazz = getClass(obj, propName);
			if (clazz != null) {
				Object valObj = convertPropStrToObj(clazz, propName, propValue);
				BeanUtils.setProperty(obj, propName, valObj);
			} else {
				log.info("Could not find property: " + propName);
			}
		}
	}

	/**
	 * Get the type of the property with given name in the given object/instance.
	 * 
	 * @param obj
	 *            object/instance
	 * @param propName
	 *            property-name
	 * @return Type of the property
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private static Class<?> getClass(Object obj, String propName) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		Class<?> clazz = PropertyUtils.getPropertyType(obj, propName);
		return clazz;
	}

	/**
	 * Get the "sub"-object; the object behind the given property-name of the given object.
	 * 
	 * @param obj
	 *            the object/instance
	 * @param propObj
	 *            the property-name
	 * @return the/a new instance of the objects property with given name
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
	private static Object getSubObject(Object obj, String propObj) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, InstantiationException {
		Object subObj = PropertyUtils.getProperty(obj, propObj);
		if (subObj == null) {
			Class<?> clazz = PropertyUtils.getPropertyType(obj, propObj);
			subObj = clazz.newInstance();
			BeanUtils.setProperty(obj, propObj, subObj);
		}
		return subObj;
	}

	/**
	 * Convert the given (property-)value to the given type.
	 * 
	 * @param clazz
	 *            the type of the property (to convert the property to)
	 * @param propName
	 *            the property-name
	 * @param propValue
	 *            the property-value
	 * @return the property-value (converted to the expected type)
	 * @throws ParseException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private static Object convertPropStrToObj(Class<?> clazz, String propName, String propValue)
			throws ParseException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			if (clazz == Integer.class || clazz == int.class) {
				return Integer.parseInt(propValue);
			} else if (clazz == Double.class || clazz == double.class) {
				return Double.parseDouble(propValue);
			} else if (clazz == Long.class || clazz == long.class) {
				return Long.parseLong(propValue);
			} else if (clazz == Float.class || clazz == float.class) {
				return Float.parseFloat(propValue);
			} else if (clazz == Boolean.class || clazz == boolean.class) {
				return Boolean.parseBoolean(propValue);
			} else if (clazz == BigDecimal.class) {
				return new BigDecimal(propValue);
			} else if (clazz == Date.class) {
				return SimpleDateFormat.getDateInstance().parse(propValue);
			} else if (clazz.isEnum()) {
				return clazz.getMethod("valueOf", String.class).invoke(clazz, propValue);
			}
			return propValue; // value of
		} catch (NumberFormatException | ParseException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1} - {2}", new Object[] {
					clazz, propName, propValue });
			throw e;
		} catch (IllegalArgumentException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1} - {2}", new Object[] {
					clazz, propName, propValue });
			throw e;
		} catch (SecurityException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1} - {2}", new Object[] {
					clazz, propName, propValue });
			throw e;
		}
	}
}
