package com.jexunit.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.jexunit.core.data.ExcelLoader;

public class GevoTestExcelLoaderTest {

	@Test
	public void type() throws Exception {
		assertThat(ExcelLoader.class, notNullValue());
	}

	@Test
	public void getColumn_A$int() throws Exception {
		int column = 1;
		String actual = ExcelLoader.getColumn(column);
		String expected = "A";
		assertThat(actual, is(equalTo(expected)));

		column = 26;
		expected = "Z";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 28;
		expected = "AB";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 52;
		expected = "AZ";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 60;
		expected = "BH";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 705;
		expected = "AAC";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 4136;
		expected = "FCB";
		actual = ExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));
	}

}
