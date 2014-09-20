package com.jexunit.core.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlRuntime;

/**
 * @author fabian
 *
 */
public class OgnlUtils {

	private static final String REGEX_EXPRESSION = "^(.*)(\\[)(.*)=(.*)(\\].*)";

	public static String prepareExpression(String expression) {
		Pattern pattern = Pattern.compile(REGEX_EXPRESSION);
		Matcher m = pattern.matcher(expression);

		if (m.matches()) {
			StringBuilder sb = new StringBuilder(m.group(1));
			sb.append(m.group(2));
			String expr = m.group(3);
			if (!expr.startsWith("'")) {
				sb.append("'");
			}
			sb.append(expr);
			sb.append("=");
			String val = m.group(4);
			sb.append(m.group(4));
			if (!val.endsWith("'")) {
				sb.append("'");
			}
			sb.append(m.group(5));
			return sb.toString();
		} else {
			return expression;
		}
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
	 * @throws OgnlException
	 *             in case that something goes wrong
	 */
	public static void setPropertyToObject(Object obj, String propName, String propValue)
			throws OgnlException {
		OgnlRuntime.setNullHandler(obj.getClass(), new InstantiatingNullHandler());
		OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
		OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
		@SuppressWarnings("rawtypes")
		Map context = Ognl.createDefaultContext(obj);
		Ognl.setTypeConverter(context, new CustomTypeConverter());

		String propertyExpression = prepareExpression(propName);
		Object expr = Ognl.parseExpression(propertyExpression);

		Ognl.setValue(expr, context, obj, propValue);
	}

	public static Object getProperty(Object obj, String propName) throws OgnlException {
		OgnlRuntime.setNullHandler(obj.getClass(), new InstantiatingNullHandler());
		OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
		OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
		@SuppressWarnings("rawtypes")
		Map context = Ognl.createDefaultContext(obj);
		Ognl.setTypeConverter(context, new CustomTypeConverter());

		String propertyExpression = prepareExpression(propName);
		Object expr = Ognl.parseExpression(propertyExpression);

		return Ognl.getValue(expr, context, obj);
	}
}
