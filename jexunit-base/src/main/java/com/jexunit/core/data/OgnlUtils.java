package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import ognl.*;

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
        @SuppressWarnings("rawtypes") final Map context =
                Ognl.createDefaultContext(
                        obj,
                        new DefaultMemberAccess(false),
                        new DefaultClassResolver(),
                        new CustomTypeConverter());

        final Object expr = parseExpression(propName);

        Ognl.setValue(expr, context, obj, propValue);
    }

    /**
     * Parse the given (property-)expression. Unless configured otherwise
     * ({@link JExUnitConfig.ConfigKey#OGNL_PROPERTY_ACCESS_ONLY}), only property access is allowed, because the
     * expressions come from the excel files.
     *
     * @param propName the (property-)expression
     * @return the parsed expression
     * @throws OgnlException if the expression cannot be parsed or is not allowed
     */
    static Object parseExpression(final String propName) throws OgnlException {
        final Object expr = Ognl.parseExpression(prepareExpression(propName));
        if (Boolean.parseBoolean(
                JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.OGNL_PROPERTY_ACCESS_ONLY))) {
            checkPropertyAccessOnly((Node) expr, propName);
        }
        return expr;
    }

    private static void checkPropertyAccessOnly(final Node node, final String propName) throws OgnlException {
        if (!(node instanceof ASTChain || node instanceof ASTProperty || node instanceof ASTConst)) {
            throw new OgnlException(String.format(
                    "Expression '%s' is not allowed: only property access is supported (found %s). Set '%s' to "
                            + "false to allow any OGNL expression.", propName, node.getClass().getSimpleName(),
                    JExUnitConfig.ConfigKey.OGNL_PROPERTY_ACCESS_ONLY.getKey()));
        }
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            checkPropertyAccessOnly(node.jjtGetChild(i), propName);
        }
    }

    public static Object getProperty(final Object obj, final String propName) throws OgnlException {
        OgnlRuntime.setNullHandler(obj.getClass(), new InstantiatingNullHandler());
        OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
        @SuppressWarnings("rawtypes") final Map context = Ognl.createDefaultContext(
                obj,
                new DefaultMemberAccess(false),
                new DefaultClassResolver(),
                new CustomTypeConverter());

        final Object expr = parseExpression(propName);

        return Ognl.getValue(expr, context, obj);
    }

}
