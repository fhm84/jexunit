package com.jexunit.core.data;

import ognl.Ognl;
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

        // iterate through the collection
        for (final Object obj : target) {
            final Object currentValue = Ognl.getValue(expression, obj);
            // if we found the object out of the collection with the expected value ...
            if (currentValue != null && currentValue.equals(expectedValue)) {
                // ... return the current object out of the collection
                return obj;
            }
        }

        return null;
    }

}
