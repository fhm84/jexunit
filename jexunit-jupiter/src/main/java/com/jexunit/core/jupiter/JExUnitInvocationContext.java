package com.jexunit.core.jupiter;

import com.jexunit.core.model.TestCase;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.Collections;
import java.util.List;

class JExUnitInvocationContext implements TestTemplateInvocationContext {

    private final List<TestCase<?>> testCases;
    private final String identifier;

    JExUnitInvocationContext(final List<TestCase<?>> testCases, final String identifier) {
        this.testCases = testCases;
        this.identifier = identifier;
    }

    @Override
    public String getDisplayName(final int invocationIndex) {
        final String name = identifier.substring(identifier.lastIndexOf('/') + 1);
        if (testCases != null && !testCases.isEmpty() && testCases.get(0).getMetadata() != null) {
            return "[" + name + " - " + testCases.get(0).getMetadata().getTestGroup() + "]";
        }
        return "[" + name + "]";
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return Collections.singletonList(new JExUnitInterceptor(testCases, identifier));
    }
}
