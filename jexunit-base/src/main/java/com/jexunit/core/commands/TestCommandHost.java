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
}
