package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.GevoTester;
import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework. This test should provide the arithmetical operations ADD and SUB.
 * 
 * @author fabian
 * 
 */
@RunWith(GevoTester.class)
public class ArithmeticalFieldTest extends GevoTestBase {

	@ExcelFile
	static String excelFile = "src/test/resources/ArithmeticalTests.xlsx";

	@BeforeClass
	public static void setup() {
		System.out.println("BeforeClass - ArithmeticTests");
	}

	@Before
	public void init() {
		System.out.println("Before - ArithmeticTests");
	}

	@Override
	public void runCommand(GevoTestCase testCase) throws Exception {
		ArithmeticalTestCommands.runCommand(testCase);
	}

	@TestCommand(value = "div")
	public static void runSubCommand(GevoTestCase testCase, ArithmeticalTestObject testObject)
			throws Exception {
		System.out.println("in test command: DIV!");
		assertThat(testObject.getParam1() / testObject.getParam2(), equalTo(testObject.getResult()));
	}
}
