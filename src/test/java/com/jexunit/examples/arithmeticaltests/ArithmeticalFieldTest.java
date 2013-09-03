package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.junit.Parameterized.ExcelFile;
import com.jexunit.examples.arithmeticaltests.model.ArithmeticalTestObject;

/**
 * Simple Test for the framework.<br>
 * <p>
 * In this test, the excel file will be provided via the static String attribute "excelFile"
 * annotated with <code>@ExcelFile</code>.
 * </p>
 * <p>
 * This test should provide the arithmetical operation DIV. This method should be preferred if there
 * are multiple methods found for the test-command "DIV".
 * </p>
 * <p>
 * All the other test-commands will be handled by the overridden {@link #runCommand(GevoTestCase)}
 * method.
 * </p>
 * 
 * @author fabian
 * 
 */
public class ArithmeticalFieldTest extends GevoTestBase {

	private static Logger log = Logger.getLogger(ArithmeticalFieldTest.class.getName());

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
	public void runCommand(GevoTestCase testCase) throws Exception {
		ArithmeticalTestCommands.runCommand(testCase);
	}

	@TestCommand(value = "div")
	public static void runDivCommand(GevoTestCase testCase, ArithmeticalTestObject testObject)
			throws Exception {
		log.info("in test command: DIV!");
		assertThat(testObject.getParam1() / testObject.getParam2(), equalTo(testObject.getResult()));
	}
}
