package com.jexunit.core.jupiter;

import com.jexunit.core.commands.TestCommandHost;
import com.jexunit.core.model.TestCase;
import org.junit.jupiter.api.extension.ExtensionContext;

class JUnit5TestCommandHost implements TestCommandHost {

    private final Object testInstance;
    private final ExtensionContext extensionContext;

    JUnit5TestCommandHost(final Object testInstance, final ExtensionContext extensionContext) {
        this.testInstance = testInstance;
        this.extensionContext = extensionContext;
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

    @Override
    public Object getTestInstance() {
        return testInstance;
    }

    @Override
    public Object getFrameworkContext() {
        return extensionContext;
    }
}
