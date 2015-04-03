package com.jexunit.core.data;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * Helper class for finding entities inside a collection using a condition. So you can access an entity out of a
 * collection by one of its attributes.
 * 
 * @author fabian
 *
 */
public class CollectionPropertyHelper {

	private static final String REGEX_CONDITION = "(.*)=(.*)";

	public static boolean matches(String propertyName) {
		return Pattern.matches(REGEX_CONDITION, propertyName);
	}

	public static Object getProperty(@SuppressWarnings("rawtypes") Map context, Collection<?> target,
			String propertyCondition) throws OgnlException {
		Pattern pattern = Pattern.compile(REGEX_CONDITION);
		Matcher m = pattern.matcher(propertyCondition);

		String expression = "";
		String expectedValue = "";

		if (m.matches()) {
			expression = m.group(1);
			expectedValue = m.group(2);
		}

		// iterate through the collection
		for (Object obj : target) {
			Object currentValue = Ognl.getValue(expression, obj);
			// if we found the object out of the collection with the expected value ...
			if (currentValue != null && currentValue.equals(expectedValue)) {
				// ... return the current object out of the collection
				return obj;
			}
		}

		return null;
	}
}
