package com.jexunit.core.commands;

import com.jexunit.core.model.TestCase;

public interface TestCommandHost {
    Class<?> getTestType();
    void runCommand(TestCase<?> testCase) throws Exception;

    /**
     * Returns the object to invoke @TestCommand methods on when the method's declaring class
     * matches the host's test instance. Returns null to fall back to creating/caching a new instance.
     */
    Object getTestClassInstance(Class<?> declaringClass) throws Exception;

    /**
     * Returns the instance of the running test class (for the {@code CommandInstanceFactory}), or null if there is
     * none.
     */
    default Object getTestInstance() {
        return null;
    }

    /**
     * Returns the context of the test framework (for the {@code CommandInstanceFactory}), like the JUnit 5
     * {@code ExtensionContext}, or null if there is none.
     */
    default Object getFrameworkContext() {
        return null;
    }
}
