package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework. This test should provide the arithmetical operations ADD and SUB.
 * 
 * @author fabian
 * 
 */
public class ArithmeticalObjectTest extends GevoTestBase {

	@ExcelFile
	static String excelFile = "src/test/resources/ArithmeticalTests.xlsx";

	@Override
	public void runCommand(GevoTestCase testCase) throws Exception {
		ArithmeticalTestCommands.runCommandWithObject(testCase);
	}
}
