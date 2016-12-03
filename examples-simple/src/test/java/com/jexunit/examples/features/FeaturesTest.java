package com.jexunit.examples.features;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.dataprovider.ExcelFile;

@Ignore("How to test the fast fail feature?")
@RunWith(JExUnit.class)
public class FeaturesTest {

	@ExcelFile
	static String[] excelFiles = new String[] { "src/test/resources/FeaturesTest.xlsx" };

}
