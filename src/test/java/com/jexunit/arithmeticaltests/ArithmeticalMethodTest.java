package com.jexunit.arithmeticaltests;

import org.junit.Before;
import org.junit.BeforeClass;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework. This test should provide the arithmetical operations ADD and SUB.
 * 
 * @author fabian
 * 
 */
public class ArithmeticalMethodTest extends GevoTestBase {

	@BeforeClass
	public static void setup() {
		System.out.println("BeforeClass - ArithmeticTests");
	}

	@Before
	public void init() {
		System.out.println("Before - ArithmeticTests");
	}

	@ExcelFile
	public static String getExcelFile() {
		return "src/test/resources/ArithmeticalTests.xlsx";
	}

	@Override
	public void runCommand(GevoTestCase testCase) throws Exception {
		ArithmeticalTestCommands.runCommand(testCase);
	}
}
