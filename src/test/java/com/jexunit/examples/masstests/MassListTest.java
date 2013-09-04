package com.jexunit.examples.masstests;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.JExUnit;
import com.jexunit.core.data.ExcelFile;

/**
 * Simple Test for the framework.
 * <p>
 * This test doesn't extend the base-class ({@link JExUnitBase}). This test works with the
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
 * <p>
 * This test should check to provide multiple excel-files via <code>List&lt;String&gt;</code> from a
 * static field.
 * </p>
 * 
 * @author fabian
 * 
 */
@RunWith(JExUnit.class)
public class MassListTest {

	@ExcelFile(worksheetAsTest = false)
	static List<String> excelFiles = new ArrayList<>();

	static {
		excelFiles.add("src/test/resources/MassTests.xlsx");
		excelFiles.add("src/test/resources/MassTests2.xlsx");
	}

}