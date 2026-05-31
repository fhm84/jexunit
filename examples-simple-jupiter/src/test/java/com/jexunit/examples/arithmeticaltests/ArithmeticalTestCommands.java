package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.model.TestCase;
import com.jexunit.examples.arithmeticaltests.model.ArithmeticalTestObject;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Provide the Test-Commands ADD and SUB for the Arithmetical tests (JUnit 5 variant).
 */
public class ArithmeticalTestCommands {

    private static final Logger log = Logger.getLogger(ArithmeticalTestCommands.class.getName());

    @TestCommand("add")
    public void runAddCommand(final TestCase<?> testCase) throws Exception {
        log.info("in test command: ADD!");
        final ArithmeticalTestObject obj = TestObjectHelper.createObject(testCase, ArithmeticalTestObject.class);
        assertEquals(obj.getParam1() + obj.getParam2(), obj.getResult());
    }

    @TestCommand({"sub", "subtract"})
    public void runSubCommand(final TestCase<?> testCase, final ArithmeticalTestObject testObject) {
        log.info("in test command: " + testCase.getTestCommand() + "!");
        assertEquals(testObject.getParam1() - testObject.getParam2(), testObject.getResult());
    }

}
