package com.jexunit.examples.arithmeticaltests;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework.
 * <p>
 * This test should provide the excel file via the static method {@link #getExcelFile()} annotated
 * with {@link @ExcelFile}.
 * </p>
 * <p>
 * All the test-commands, that are not found in the classpath (methods annotated with the
 * <code>@TestCommand</code>-Annotation), will be handled by the overridden
 * {@link #runCommand(GevoTestCase)} method.
 * </p>
 * 
 * @author fabian
 * 
 */
public class ArithmeticalMethodTest extends GevoTestBase {

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
	public void runCommand(GevoTestCase testCase) throws Exception {
		log.log(Level.INFO, "running test-command: {0}", testCase.getTestCommand());
		ArithmeticalTestCommands.runCommand(testCase);
	}
}
