package com.jexunit.examples.arithmeticaltests;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;

/**
 * Simple Test for the framework. This test should provide the arithmetical operations ADD and SUB.
 * 
 * @author fabian
 * 
 */
public class ArithmeticalObjectTest extends JExUnitBase {

	@ExcelFile
	static String excelFile = "src/test/resources/ArithmeticalTests.xlsx";

	@Override
	public void runCommand(TestCase testCase) throws Exception {
		ArithmeticalTestCommands.runCommandWithObject(testCase);
	}
}
