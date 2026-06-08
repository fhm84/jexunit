package com.jexunit.core.jupiter;

import com.jexunit.core.commands.TestCommandHost;
import com.jexunit.core.model.TestCase;

class JUnit5TestCommandHost implements TestCommandHost {

    private final Object testInstance;

    JUnit5TestCommandHost(final Object testInstance) {
        this.testInstance = testInstance;
    }

    @Override
    public Class<?> getTestType() {
        return testInstance.getClass();
    }

    @Override
    public void runCommand(final TestCase<?> testCase) throws Exception {
        throw new NoSuchMethodError(String.format(
                "No implementation found for the command \"%1$s\". "
                + "Please provide a method or class annotated with @TestCommand(\"%1$s\").",
                testCase.getTestCommand()));
    }

    @Override
    public Object getTestClassInstance(final Class<?> declaringClass) throws Exception {
        return declaringClass.isInstance(testInstance) ? testInstance : null;
    }
}
