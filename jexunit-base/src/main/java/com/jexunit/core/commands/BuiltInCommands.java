package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.data.ObjectVerifier;
import com.jexunit.core.data.VariableResolver;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in commands run by the {@link TestCommandRunner}: <code>set</code> and <code>verify</code>. They only run
 * if there is no command implementation with the same name. (The built-in commands <code>disabled</code> and
 * <code>report</code> are handled by the runners.)
 *
 * @author fabian
 */
final class BuiltInCommands {

    private BuiltInCommands() {
    }

    /**
     * Run the given test case, if it is a built-in command.
     *
     * @param testCase the test case
     * @return true, if the test case is a built-in command (and was run), else false
     * @throws AssertionError        if a verification fails
     * @throws TestCommandSetupError if the command cannot be run, e.g. the object to verify is not found
     */
    static boolean run(final TestCase<?> testCase) {
        final String command = testCase.getTestCommand();
        if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.SET).equalsIgnoreCase(command)) {
            set(testCase);
            return true;
        } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.VERIFY).equalsIgnoreCase(command)) {
            verify(testCase);
            return true;
        }
        return false;
    }

    /**
     * Check, whether the given command is a built-in command (respecting the configured names and prefix).
     *
     * @param command the command
     * @return true, if it is a built-in command
     */
    static boolean isBuiltIn(final String command) {
        for (final DefaultCommands dc : new DefaultCommands[]{DefaultCommands.DISABLED, DefaultCommands.REPORT,
                DefaultCommands.SET, DefaultCommands.VERIFY}) {
            if (JExUnitConfig.getDefaultCommandProperty(dc).equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The <code>set</code> command: add a variable for each column (the header is its name) with the value of the cell
     * to the TestContext. Empty cells are skipped.
     *
     * @param testCase the test case of the set command
     */
    private static void set(final TestCase<?> testCase) {
        for (final Map<String, TestCell> row : testCase.getMultilineValues()) {
            for (final Map.Entry<String, TestCell> entry : row.entrySet()) {
                final String value = entry.getValue().getValue();
                if (value != null && !value.isEmpty()) {
                    TestContextManager.add(entry.getKey(), value);
                }
            }
        }
    }

    /**
     * The <code>verify</code> command: compare the properties of an object out of the TestContext (selected by the
     * <code>object</code> column) with the expected values of the other columns. All the mismatches (of all rows of a
     * multiline command) are reported in one assertion error. A row without <code>object</code> column verifies the
     * object of the row before.
     *
     * @param testCase the test case of the verify command
     */
    private static void verify(final TestCase<?> testCase) {
        final String objectColumn = JExUnitConfig.getDefaultCommandProperty(DefaultCommands.VERIFY_OBJECT);
        final List<String> mismatches = new ArrayList<>();
        String objectName = null;
        for (final Map<String, TestCell> row : testCase.getMultilineValues()) {
            final Map<String, TestCell> expectedValues = new LinkedHashMap<>();
            for (final Map.Entry<String, TestCell> entry : row.entrySet()) {
                if (objectColumn.equalsIgnoreCase(entry.getKey())) {
                    final String value = entry.getValue().getValue();
                    if (value != null && !value.isEmpty()) {
                        objectName = value;
                    }
                } else {
                    expectedValues.put(entry.getKey(), entry.getValue());
                }
            }
            if (objectName == null) {
                throw new TestCommandSetupError(String.format(
                        "Test command '%s' could not be prepared: the column '%s' with the id of the object to verify "
                                + "(in the TestContext) is missing or empty", testCase.getTestCommand(), objectColumn));
            }

            final Object object;
            try {
                object = VariableResolver.getValue(objectName, TestContextManager.getTestContext());
            } catch (final IllegalArgumentException e) {
                throw new TestCommandSetupError(String.format("Test command '%s' could not be prepared: %s",
                        testCase.getTestCommand(), e.getMessage()), e);
            }
            if (object == null) {
                throw new AssertionError(String.format("Verification of '%s' failed: the object is null", objectName));
            }
            final List<String> rowMismatches;
            try {
                rowMismatches = ObjectVerifier.verify(object, expectedValues);
            } catch (final IllegalArgumentException e) {
                // the row can't check anything, so it must not satisfy an expected exception
                throw new TestCommandSetupError(String.format("Test command '%s' could not be prepared: %s",
                        testCase.getTestCommand(), e.getMessage()), e);
            }
            for (final String mismatch : rowMismatches) {
                mismatches.add(objectName + "." + mismatch);
            }
        }
        if (!mismatches.isEmpty()) {
            throw new AssertionError(String.format("Verification failed (%d mismatch%s):%n  %s", mismatches.size(),
                    mismatches.size() == 1 ? "" : "es", String.join(String.format("%n  "), mismatches)));
        }
    }

}
