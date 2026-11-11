package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.model.TestCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares the properties of an object with the expected values out of the excel file (used by the built-in
 * <code>verify</code> command). The keys are property paths (like <code>customer.name</code> or
 * <code>items[id=5].qty</code>), read without changing the object (see {@link OgnlUtils#readProperty(Object, String)}).
 * The expected value is converted to the type of the property before comparing (see
 * {@link PropertyUtils#isEqual(Object, String)}).
 * <ul>
 * <li>An empty cell is not checked.</li>
 * <li>The configured null value ({@link JExUnitConfig.ConfigKey#VERIFY_NULL}, <code>&lt;null&gt;</code> by default)
 * expects <code>null</code>.</li>
 * </ul>
 *
 * @author fabian
 */
public final class ObjectVerifier {

    private ObjectVerifier() {
    }

    /**
     * Compare the properties of the given object with the expected values.
     *
     * @param object         the object to verify
     * @param expectedValues the expected values: the key is the property path, the cell the expected value
     * @return a description of each mismatch, like <code>customer.name (C5): expected "Miller" but was "Müller"</code>
     * (empty, if all the values match)
     */
    public static List<String> verify(final Object object, final Map<String, TestCell> expectedValues) {
        final String nullValue = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.VERIFY_NULL);
        final List<String> mismatches = new ArrayList<>();
        for (final Map.Entry<String, TestCell> entry : expectedValues.entrySet()) {
            final String expected = entry.getValue().getValue();
            if (expected == null || expected.isEmpty()) {
                continue;
            }
            final String path = entry.getKey();
            final String location = entry.getValue().getIdentifier() != null
                    ? path + " (" + entry.getValue().getIdentifier() + ")" : path;

            final Object actual;
            try {
                actual = OgnlUtils.readProperty(object, path);
            } catch (final Exception e) {
                mismatches.add(String.format("%s: cannot be read: %s", location, e.getMessage()));
                continue;
            }

            if (expected.equals(nullValue)) {
                if (actual != null) {
                    mismatches.add(String.format("%s: expected %s but was %s", location, nullValue,
                            describe(actual, nullValue)));
                }
            } else if (!PropertyUtils.isEqual(actual, expected)) {
                mismatches.add(String.format("%s: expected \"%s\" but was %s", location, expected,
                        describe(actual, nullValue)));
            }
        }
        return mismatches;
    }

    private static String describe(final Object value, final String nullValue) {
        return value == null ? nullValue : "\"" + PropertyUtils.convertObjectToPropertyString(value) + "\"";
    }

}
