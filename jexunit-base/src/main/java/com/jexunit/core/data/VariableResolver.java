package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

import java.util.Map;

/**
 * Resolves the variables in the cell values: a placeholder like <code>${orderId}</code> is replaced by the value added
 * to the {@link TestContext} with this id, e.g. by an earlier command (<code>ctx.add("orderId", id)</code>) or the
 * built-in <code>set</code> command. The variables are resolved when the command runs, not when the file is loaded.
 * <ul>
 * <li>A property path reads a property of the value: <code>${order.customer.name}</code>,
 * <code>${order.items[0].qty}</code>. Like the column headers, only property access is allowed (see
 * {@link JExUnitConfig.ConfigKey#OGNL_PROPERTY_ACCESS_ONLY}). The longest prefix that is an id in the TestContext is
 * the variable, the rest is the path.</li>
 * <li>The value is converted to a string the way it would be written in the excel file (e.g. dates with the configured
 * pattern), so the usual conversion to the type of the parameter or property applies.</li>
 * <li><code>$${</code> is a literal <code>${</code>.</li>
 * <li>A cell that only contains a variable whose value is <code>null</code> (e.g. a <code>null</code> property) has no
 * value. Inside of a text, <code>null</code> is an empty string.</li>
 * <li>An unknown variable fails the row.</li>
 * </ul>
 * The resolution can be turned off with {@link JExUnitConfig.ConfigKey#VARIABLES_ENABLED}.
 *
 * @author fabian
 */
public final class VariableResolver {

    private static final String START = "${";
    private static final String ESCAPED_START = "$${";
    private static final char END = '}';
    /**
     * Marker for a name that is not found in the TestContext (as <code>null</code> is a valid value of a property).
     */
    private static final Object UNKNOWN = new Object();

    private VariableResolver() {
    }

    /**
     * Check whether variables are resolved (see {@link JExUnitConfig.ConfigKey#VARIABLES_ENABLED}).
     *
     * @return true, if the variables are resolved
     */
    public static boolean isEnabled() {
        return Boolean.parseBoolean(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.VARIABLES_ENABLED));
    }

    /**
     * Resolve the variables in all the values of the given test case (all rows of a multiline test case) from the
     * current {@link TestContext}. The values of the cells are replaced. Nothing is done, if the resolution is turned
     * off.
     *
     * @param testCase the test case
     * @throws IllegalArgumentException if a variable is unknown or cannot be read
     */
    public static void resolve(final TestCase<?> testCase) {
        if (!isEnabled()) {
            return;
        }
        final TestContext context = TestContextManager.getTestContext();
        for (final Map<String, TestCell> row : testCase.getMultilineValues()) {
            for (final TestCell cell : row.values()) {
                cell.setValue(resolve(cell.getValue(), cell.getIdentifier(), context));
            }
        }
    }

    /**
     * Resolve the variables in the given value from the given {@link TestContext}. This is done regardless of
     * {@link JExUnitConfig.ConfigKey#VARIABLES_ENABLED}.
     *
     * @param value   the value (of a cell)
     * @param cell    the cell (like <code>C5</code>) for the error messages, may be <code>null</code>
     * @param context the TestContext to look up the variables
     * @return the value with the variables replaced
     * @throws IllegalArgumentException if a variable is unknown or cannot be read
     */
    public static String resolve(final String value, final String cell, final TestContext context) {
        if (value == null || !value.contains(START)) {
            return value;
        }

        // a cell that only contains one variable
        if (value.startsWith(START) && value.indexOf(END) == value.length() - 1) {
            return PropertyUtils.convertObjectToPropertyString(
                    lookup(value.substring(START.length(), value.length() - 1), value, cell, context));
        }

        final StringBuilder sb = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            if (value.startsWith(ESCAPED_START, i)) {
                sb.append(START);
                i += ESCAPED_START.length();
            } else if (value.startsWith(START, i)) {
                final int end = value.indexOf(END, i + START.length());
                if (end < 0) {
                    throw new IllegalArgumentException(String.format(
                            "Variable not closed in %s: '%s' (write '%s' for a literal '%s')",
                            describe(cell), value, ESCAPED_START, START));
                }
                final Object resolved = lookup(value.substring(i + START.length(), end), value, cell, context);
                if (resolved != null) {
                    sb.append(PropertyUtils.convertObjectToPropertyString(resolved));
                }
                i = end + 1;
            } else {
                sb.append(value.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Look up the value of the given variable: the longest prefix of the name, that is an id in the TestContext, is
     * the variable, the rest of the name is a property path on its value.
     *
     * @param variable the variable (the name between the braces)
     * @param value    the value of the cell (for the error messages)
     * @param cell     the cell (for the error messages)
     * @param context  the TestContext
     * @return the value of the variable (may be <code>null</code> for a property path)
     */
    private static Object lookup(final String variable, final String value, final String cell,
                                 final TestContext context) {
        final String name = variable.trim();
        final Object found;
        try {
            found = find(name, context);
        } catch (final Exception e) {
            throw new IllegalArgumentException(String.format("Variable '%s%s%s' in %s cannot be read: %s", START, name,
                    END, describe(cell), e.getMessage()), e);
        }
        if (found == UNKNOWN) {
            throw new IllegalArgumentException(String.format(
                    "Unknown variable '%s%s%s' in %s: '%s'. Nothing was added to the TestContext with this id (write "
                            + "'%s' for a literal '%s')", START, name, END, describe(cell), value, ESCAPED_START,
                    START));
        }
        return found;
    }

    /**
     * Get the value with the given name out of the given {@link TestContext}, like a variable (without the braces):
     * the value added with this id, or a property of it (like <code>order.customer</code>).
     *
     * @param name    the id of the value, optionally followed by a property path
     * @param context the TestContext
     * @return the value (may be <code>null</code> for a property path)
     * @throws IllegalArgumentException if nothing was added with this id, or the property cannot be read
     */
    public static Object getValue(final String name, final TestContext context) {
        final String trimmed = name == null ? "" : name.trim();
        final Object found;
        try {
            found = find(trimmed, context);
        } catch (final Exception e) {
            throw new IllegalArgumentException(String.format("'%s' cannot be read: %s", trimmed, e.getMessage()), e);
        }
        if (found == UNKNOWN) {
            throw new IllegalArgumentException(String.format(
                    "Nothing was added to the TestContext with the id '%s'", trimmed));
        }
        return found;
    }

    /**
     * Find the value with the given name: the longest prefix of the name, that is an id in the TestContext, is the
     * value, the rest of the name is a property path on it.
     *
     * @param name    the (trimmed) name
     * @param context the TestContext
     * @return the value (may be <code>null</code> for a property path), or {@link #UNKNOWN}
     * @throws Exception if the property cannot be read
     */
    private static Object find(final String name, final TestContext context) throws Exception {
        if (name.isEmpty()) {
            return UNKNOWN;
        }
        final Object found = context.get(Object.class, name);
        if (found != null) {
            return found;
        }
        for (int i = name.length() - 1; i > 0; i--) {
            final char c = name.charAt(i);
            if (c != '.' && c != '[') {
                continue;
            }
            final Object root = context.get(Object.class, name.substring(0, i));
            if (root != null) {
                // "order.id" -> "id", "items[0].qty" -> "[0].qty"
                return OgnlUtils.readProperty(root, c == '.' ? name.substring(i + 1) : name.substring(i));
            }
        }
        return UNKNOWN;
    }

    private static String describe(final String cell) {
        return cell != null ? "cell " + cell : "value";
    }

}
