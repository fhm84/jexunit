package com.jexunit.core;

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

/**
 * Hilfsklasse, um aus den Excel-Files generisch die Entity-Klassen zu generieren. Im Excel sind
 * lediglich die Attributnamen und die gewünschten Werte anzugeben.
 * 
 * @author fabian
 * 
 */
public class GevoTestObjectHelper {

	private static Logger log = Logger.getLogger(GevoTestObjectHelper.class.getName());

	private static final String PROP_DEL = ".";

	/**
	 * Erzeugt eine neue Instanz der angegebenen Klasse und befüllt die im TestCase angegebenen
	 * Attribute mit den entsprechenden Werten.
	 * 
	 * @param testCase
	 *            TestCase, der die zu setzenden Attribute (und Werte) enthält
	 * @param clazz
	 *            Klasse (Entität), die erstellt werden soll
	 * @return einen neue Instanz der angegebenen Klasse
	 * @throws Exception
	 */
	public static <T> T createObject(GevoTestCase testCase, Class<T> clazz) throws Exception {
		T obj = clazz.newInstance();

		for (Map.Entry<String, GevoTestCell> entry : testCase.getValues().entrySet()) {
			setPropertyToObject(obj, entry.getKey(), entry.getValue().getValue());
		}
		return obj;
	}

	/**
	 * Ändert die im TestCase übergebenen Werte des Objekts.
	 * 
	 * @param testCase
	 *            TestCase, der die zu ändernden Attribute (und Werte) enthält
	 * @param object
	 *            konkretes Objekt, dessen Attribut-Werte geändert werden sollen
	 * @return übergebenes Objekt mit geänderten Werten
	 * @throws Exception
	 */
	public static <T> T createObject(GevoTestCase testCase, T object) throws Exception {
		for (Map.Entry<String, GevoTestCell> entry : testCase.getValues().entrySet()) {
			setPropertyToObject(object, entry.getKey(), entry.getValue().getValue());
		}
		return object;
	}

	/**
	 * Setzt das Attribut (Property) des angegebenen Objekts auf den angegebenen Wert.
	 * 
	 * @param obj
	 *            Objekt
	 * @param propName
	 *            Property-Name
	 * @param propValue
	 *            Property-Wert
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
	 * Gibt den Typ (die Klasse) des Properties mit dem angegebenen Namen in dem angegebenen Objekt
	 * zurück.
	 * 
	 * @param obj
	 *            Objekt
	 * @param propName
	 *            Property-Name
	 * @return Klasse (Typ) des Properties
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private static Class<?> getClass(Object obj, String propName) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		Class<?> clazz = PropertyUtils.getPropertyType(obj, propName);
		return clazz;
	}

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
	 * Konvertiert den Property-Wert in den angegebenen Typ (Klasse).
	 * 
	 * @param clazz
	 *            Klasse (Typ) des Properties
	 * @param propName
	 *            Property-Name
	 * @param propValue
	 *            Property-Wert
	 * @return Property-Wert (in den richtigen Typ konvertiert)
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
