package com.jexunit.core.commands;

import com.jexunit.core.model.TestCase;

public interface TestCommandHost {
    Class<?> getTestType();
    void runCommand(TestCase<?> testCase) throws Exception;
}
