package com.jexunit.core.jupiter;

import com.jexunit.core.model.TestCase;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.Collections;
import java.util.List;

class JExUnitInvocationContext implements TestTemplateInvocationContext {

    private final List<TestCase<?>> testCases;
    private final String identifier;
    /**
     * The test group (worksheet) is split into several invocations (e.g. one per command row with
     * {@code @ExcelFile(worksheetAsTest = false)}), so the display name has to name the command as well.
     */
    private final boolean nameCommand;

    JExUnitInvocationContext(final List<TestCase<?>> testCases, final String identifier, final boolean nameCommand) {
        this.testCases = testCases;
        this.identifier = identifier;
        this.nameCommand = nameCommand;
    }

    @Override
    public String getDisplayName(final int invocationIndex) {
        final String name = identifier.substring(Math.max(identifier.lastIndexOf('/'), identifier.lastIndexOf('\\')) + 1);
        if (testCases == null || testCases.isEmpty() || testCases.get(0).getMetadata() == null) {
            return "[" + name + "]";
        }
        final TestCase<?> testCase = testCases.get(0);
        final String displayName = "[" + name + " - " + testCase.getMetadata().getTestGroup() + "]";
        if (!nameCommand) {
            return displayName;
        }
        // e.g. "[tests.xlsx - orders] A12: createOrder"
        final String cell = testCase.getMetadata().getIdentifier();
        return displayName + " " + (cell != null ? cell + ": " : "") + testCase.getTestCommand();
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return Collections.singletonList(new JExUnitInterceptor(testCases, identifier));
    }
}
