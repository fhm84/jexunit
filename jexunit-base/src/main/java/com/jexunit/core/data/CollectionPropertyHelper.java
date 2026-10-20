package com.jexunit.core.data;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for finding entities inside a collection using a condition. So you can access an entity out of a
 * collection by one of its attributes.
 *
 * @author fabian
 */
public class CollectionPropertyHelper {

    private static final String REGEX_CONDITION = "(.*)=(.*)";

    public static boolean matches(final String propertyName) {
        return Pattern.matches(REGEX_CONDITION, propertyName);
    }

    public static Object getProperty(@SuppressWarnings("rawtypes") final Map context, final Collection<?> target,
                                     final String propertyCondition) throws OgnlException {
        final Pattern pattern = Pattern.compile(REGEX_CONDITION);
        final Matcher m = pattern.matcher(propertyCondition);

        String expression = "";
        String expectedValue = "";

        if (m.matches()) {
            expression = m.group(1);
            expectedValue = m.group(2);
        }

        // the condition is part of the column header, so it has to pass the same checks as the header itself
        final Object parsedExpression = OgnlUtils.parseExpression(expression);

        // Ognl.getValue replaces the root object and the current node of the context, so restore them afterwards (they
        // are still needed to resolve the property types of the rest of the expression and for error messages)
        final OgnlContext ognlContext = (OgnlContext) context;
        final Object root = ognlContext.getRoot();
        final Node currentNode = ognlContext.getCurrentNode();
        try {
            // iterate through the collection
            for (final Object obj : target) {
                final Object currentValue = Ognl.getValue(parsedExpression, context, obj);
                // if we found the object out of the collection with the expected value ...
                if (isEqual(currentValue, expectedValue)) {
                    // ... return the current object out of the collection
                    return obj;
                }
            }
        } finally {
            ognlContext.setRoot(root);
            ognlContext.setCurrentNode(currentNode);
        }

        return null;
    }

    /**
     * Check if the current value equals the expected value (out of the condition). The expected value is converted to
     * the type of the current value, so conditions also work on non-string properties (numbers, enums, ...).
     *
     * @param currentValue  the value of the collection element
     * @param expectedValue the expected value (as string)
     * @return true, if the values are equal, else false
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean isEqual(final Object currentValue, final String expectedValue) {
        if (currentValue == null) {
            return false;
        }
        if (currentValue instanceof String) {
            return currentValue.equals(expectedValue);
        }
        try {
            final Object converted = PropertyUtils.convertPropertyStringToObject(currentValue.getClass(), expectedValue);
            if (converted instanceof Comparable && converted.getClass() == currentValue.getClass()) {
                // compareTo (instead of equals) to ignore the scale of BigDecimals
                return ((Comparable) converted).compareTo(currentValue) == 0;
            }
            return currentValue.equals(converted);
        } catch (final Exception e) {
            // the expected value cannot be converted to the type of the current value, so they can't be equal
            return false;
        }
    }

}
