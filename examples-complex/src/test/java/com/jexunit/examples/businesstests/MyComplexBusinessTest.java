package com.jexunit.examples.businesstests;

import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.dataprovider.ExcelFile;

/**
 * This is a more complex example for the JExUnit-Framework using as much features as possible! (see
 * the TestCommand-Implementations).
 * 
 * @author fabian
 * 
 */
@RunWith(JExUnit.class)
public class MyComplexBusinessTest {

	@ExcelFile
	private static final String[] excelFiles = new String[] { "src/test/resources/ComplexBusinessTest.xlsx" };
}
