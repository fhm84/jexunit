package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple Test for the framework.
 * <p>
 * This test should provide the excel file via the static method {@link #getExcelFile()} annotated with
 * <code>&#064;ExcelFile</code>.
 * </p>
 * <p>
 * All the test-commands, that are not found in the classpath (methods annotated with the <code>@TestCommand</code>
 * -Annotation), will be handled by the overridden {@link #runCommand(TestCase)} method.
 * </p>
 *
 * @author fabian
 */
public class ArithmeticalMethodTest extends JExUnitBase {

    private static final Logger log = Logger.getLogger(ArithmeticalMethodTest.class.getName());

    @BeforeClass
    public static void setup() {
        log.info("BeforeClass - ArithmeticTests");
    }

    @Before
    public void init() {
        log.info("Before - ArithmeticTests");
    }

    @ExcelFile
    public static String getExcelFile() {
        return "src/test/resources/ArithmeticalTests.xlsx";
    }

    @Override
    public void runCommand(final TestCase<?> testCase) throws Exception {
        log.log(Level.INFO, "running test-command: {0}", testCase.getTestCommand());
        ArithmeticalTestCommands.runCommand(testCase);
    }

}
