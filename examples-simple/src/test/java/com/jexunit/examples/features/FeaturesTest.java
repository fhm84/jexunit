package com.jexunit.examples.features;

import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.dataprovider.ExcelFile;

@RunWith(JExUnit.class)
public class FeaturesTest {

	@ExcelFile
	static String[] excelFiles = new String[] { "src/test/resources/FeaturesTest.xlsx" };
}
