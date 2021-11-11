package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import com.jexunit.examples.arithmeticaltests.model.ArithmeticalTestObject;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Simple Test for the framework.<br>
 * <p>
 * In this test, the excel file will be provided via the static String attribute "excelFile" annotated with
 * <code>@ExcelFile</code>.
 * </p>
 * <p>
 * This test should provide the arithmetical operation DIV. This method should be preferred if there are multiple
 * methods found for the test-command "DIV".
 * </p>
 * <p>
 * All the other test-commands will be handled by the overridden {@link #runCommand(TestCase)} method.
 * </p>
 *
 * @author fabian
 */
public class ArithmeticalFieldTest extends JExUnitBase {

    private static final Logger log = Logger.getLogger(ArithmeticalFieldTest.class.getName());

    @ExcelFile
    static String excelFile = "src/test/resources/ArithmeticalTests.xlsx";

    @BeforeClass
    public static void setup() {
        log.info("BeforeClass - ArithmeticTests");
    }

    @Before
    public void init() {
        log.info("Before - ArithmeticTests");
    }

    @Override
    public void runCommand(final TestCase<?> testCase) throws Exception {
        ArithmeticalTestCommands.runCommand(testCase);
    }

    @TestCommand(value = "div")
    public static void runDivCommand(final TestCase<?> testCase, final ArithmeticalTestObject testObject) {
        log.info("in test command: DIV!");
        assertThat(testObject.getParam1() / testObject.getParam2(), equalTo(testObject.getResult()));
    }

}
