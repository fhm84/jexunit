package com.jexunit.examples.masstests;

import org.junit.runner.RunWith;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTester;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * Simple Test for the framework.
 * <p>
 * This test doesn't extend the base-class ({@link GevoTestBase}). This test works with the
 * <code>@RunWith(GevoTester.class)</code>-Annotation as the integration point for the framework.
 * </p>
 * <p>
 * In this test, all the test-command found in the excel file should be executed as a single test.
 * Per default, all the test-commands inside a excel-worksheet will be executed as a single test.
 * But you can also configure the framework to execute each test-command of an excel-file as single
 * test (for mass-testing).
 * </p>
 * <p>
 * Additionally this test should test the formula evaluation. Before reading the cell values from
 * the excel file, the formulas should be evaluated. So it should be possible to generate mass tests
 * using excel!
 * </p>
 * 
 * @author fabian
 * 
 */
@RunWith(GevoTester.class)
public class MassTest {

	@ExcelFile(worksheetAsTest = false)
	static String excelFile = "src/test/resources/MassTests.xlsx";
}