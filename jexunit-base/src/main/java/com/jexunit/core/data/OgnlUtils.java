package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import ognl.*;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fabian
 */
public class OgnlUtils {

    private static final NullHandler NULL_HANDLER = new InstantiatingNullHandler();

    /**
     * A condition inside of brackets, like <code>[name=John]</code> (but not an already quoted one like
     * <code>['name=John']</code>).
     */
    private static final Pattern CONDITION = Pattern.compile("\\[([^\\[\\]=']+)=([^\\[\\]]*)\\]");

    /**
     * Prepare the given expression for OGNL: conditions like <code>items[name=John]</code> are quoted
     * (<code>items['name=John']</code>), so OGNL passes them as a string to the collection property accessors.
     *
     * @param expression the expression (out of the column header)
     * @return the expression with quoted conditions
     */
    public static String prepareExpression(final String expression) {
        final Matcher m = CONDITION.matcher(expression);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("['" + m.group(1) + "=" + m.group(2) + "']"));
        }
        m.appendTail(sb);
        return sb.toString();
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
        OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
        final OgnlContext context =
                Ognl.createDefaultContext(
                        obj,
                        new PublicMemberAccess(),
                        new DefaultClassResolver(),
                        new CustomTypeConverter());

        final Object expr = parseExpression(propName);
        registerNullHandlers(obj.getClass(), (Node) expr);

        Ognl.setValue(expr, context, obj, propValue);
    }

    /**
     * Register the null handler for the type of the root object and the types of the (sub-)entities along the given
     * expression, so <code>null</code> (sub-)entities are created at every level of the expression.
     *
     * @param rootType   the type of the root object
     * @param expression the (parsed) expression
     */
    private static void registerNullHandlers(final Class<?> rootType, final Node expression) {
        OgnlRuntime.setNullHandler(rootType, NULL_HANDLER);
        for (int i = 1; i < PropertyTypes.length(expression); i++) {
            final Class<?> type = PropertyTypes.rawClass(PropertyTypes.resolve(rootType, expression, i));
            // only for the classes of the model, as the null handler is registered globally (not for the JDK classes)
            if (type != null && type.getClassLoader() != null) {
                OgnlRuntime.setNullHandler(type, NULL_HANDLER);
            }
        }
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
        OgnlRuntime.setPropertyAccessor(List.class, new CustomListPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(Set.class, new CustomSetPropertyAccessor());
        final OgnlContext context = Ognl.createDefaultContext(
                obj,
                new PublicMemberAccess(),
                new DefaultClassResolver(),
                new CustomTypeConverter());

        final Object expr = parseExpression(propName);
        registerNullHandlers(obj.getClass(), (Node) expr);

        return Ognl.getValue(expr, context, obj);
    }

}
