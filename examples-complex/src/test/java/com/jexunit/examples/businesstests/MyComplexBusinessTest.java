package com.jexunit.examples.businesstests;

import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.data.ExcelFile;

/**
 * @author fabian
 * 
 */
@RunWith(JExUnit.class)
public class MyComplexBusinessTest {

	@ExcelFile
	private static final String[] excelFiles = new String[] { "src/test/resources/ComplexBusinessTest.xlsx" };
}
