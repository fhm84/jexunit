package com.jexunit.core.data;

import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.Collection;
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

    public static Object getProperty(final OgnlContext context, final Collection<?> target,
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
        final Object root = context.getRoot();
        final Node currentNode = context.getCurrentNode();
        try {
            // iterate through the collection
            for (final Object obj : target) {
                final Object currentValue = Ognl.getValue(parsedExpression, context, obj);
                // if we found the object out of the collection with the expected value ...
                if (PropertyUtils.isEqual(currentValue, expectedValue)) {
                    // ... return the current object out of the collection
                    return obj;
                }
            }
        } finally {
            context.setRoot(root);
            context.setCurrentNode(currentNode);
        }

        return null;
    }

}
