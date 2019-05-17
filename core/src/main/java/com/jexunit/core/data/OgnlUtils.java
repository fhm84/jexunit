package com.jexunit.core.data;

import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fabian
 */
public class OgnlUtils {

    private static final String REGEX_EXPRESSION = "^(.*)(\\[)(.*)=(.*)(\\].*)";

    public static String prepareExpression(final String expression) {
        final Pattern pattern = Pattern.compile(REGEX_EXPRESSION);
        final Matcher m = pattern.matcher(expression);

        if (m.matches()) {
            final StringBuilder sb = new StringBuilder(m.group(1));
            sb.append(m.group(2));
            final String expr = m.group(3);
            if (!expr.startsWith("'")) {
                sb.append("'");
            }
            sb.append(expr);
            sb.append("=");
            final String val = m.group(4);
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
     * @param obj       object/instance
     * @param propName  property-name
     * @param propValue property-value
     * @throws OgnlException in case that something goes wrong
     */
    public static void setPropertyToObject(final Object obj, final String propName, final String propValue) throws OgnlException {
        OgnlRuntime.setNullHandler(obj.getClass(), new InstantiatingNullHandler());
        OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
        @SuppressWarnings("rawtypes") final Map context = Ognl.createDefaultContext(obj);
        Ognl.setTypeConverter(context, new CustomTypeConverter());

        final String propertyExpression = prepareExpression(propName);
        final Object expr = Ognl.parseExpression(propertyExpression);

        Ognl.setValue(expr, context, obj, propValue);
    }

    public static Object getProperty(final Object obj, final String propName) throws OgnlException {
        OgnlRuntime.setNullHandler(obj.getClass(), new InstantiatingNullHandler());
        OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
        @SuppressWarnings("rawtypes") final Map context = Ognl.createDefaultContext(obj);
        Ognl.setTypeConverter(context, new CustomTypeConverter());

        final String propertyExpression = prepareExpression(propName);
        final Object expr = Ognl.parseExpression(propertyExpression);

        return Ognl.getValue(expr, context, obj);
    }

}
