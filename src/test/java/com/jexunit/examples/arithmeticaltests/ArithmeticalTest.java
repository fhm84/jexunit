package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jexunit.core.GevoTestCase;
import com.jexunit.core.GevoTester;
import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework. This test should provide the arithmetical operations MUL and DIV.
 * The operations ADD and SUB will be provided by another ExcelCommandProvider.
 * 
 * @author fabian
 * 
 */
@RunWith(GevoTester.class)
public class ArithmeticalTest {

	private static Logger log = Logger.getLogger(ArithmeticalTest.class.getName());

	@ExcelFile
	static String excelFile = "src/test/resources/ArithmeticalTests.xlsx";

	@Before
	public void init() {
		log.log(Level.INFO, "BeforeClass - ArithmeticTests");
	}

	@TestCommand(value = "mul")
	public static void runMulCommand(GevoTestCase testCase, ArithmeticalTestObject testObject)
			throws Exception {
		log.log(Level.INFO, "in test command: MUL!");
		assertThat(testObject.getParam1() * testObject.getParam2(), equalTo(testObject.getResult()));
	}

	@TestCommand(value = "div")
	public static void runDivCommand(GevoTestCase testCase, ArithmeticalTestObject testObject)
			throws Exception {
		log.log(Level.INFO, "in test command: DIV!");
		assertThat(testObject.getParam1() / testObject.getParam2(), equalTo(testObject.getResult()));
	}

	@Test
	public void simpleTest() {
		System.out.println("What about this test?");
	}
}