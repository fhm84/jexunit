package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.jupiter.ExcelTest;
import com.jexunit.core.jupiter.JExUnitExtension;
import com.jexunit.core.model.TestCase;
import com.jexunit.examples.arithmeticaltests.model.ArithmeticalTestObject;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit 5 equivalent of ArithmeticalTest, demonstrating @ExtendWith(JExUnitExtension.class)
 * as the replacement for @RunWith(JExUnit.class).
 */
@ExtendWith(JExUnitExtension.class)
public class ArithmeticalJUnit5Test {

    private static final Logger log = Logger.getLogger(ArithmeticalJUnit5Test.class.getName());

    @ExcelFile
    static String[] excelFiles = new String[]{
            "src/test/resources/ArithmeticalTests.xlsx",
            "src/test/resources/ArithmeticalTests2.xlsx"
    };

    @ExcelTest
    void test() {}

    @TestCommand("mul")
    public static void runMulCommand(final TestCase<?> testCase, final ArithmeticalTestObject testObject) {
        log.log(Level.INFO, "in test command: MUL!");
        assertEquals(testObject.getParam1() * testObject.getParam2(), testObject.getResult());
    }

    @TestCommand("div")
    public static void runDivCommand(final TestCase<?> testCase, final ArithmeticalTestObject testObject) {
        log.log(Level.INFO, "in test command: DIV!");
        assertEquals(testObject.getParam1() / testObject.getParam2(), testObject.getResult());
    }
}
