package com.jexunit.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class GevoTestExcelLoaderTest {

	@Test
	public void type() throws Exception {
		assertThat(GevoTestExcelLoader.class, notNullValue());
	}

	@Test
	public void getColumn_A$int() throws Exception {
		int column = 1;
		String actual = GevoTestExcelLoader.getColumn(column);
		String expected = "A";
		assertThat(actual, is(equalTo(expected)));

		column = 26;
		expected = "Z";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 28;
		expected = "AB";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 52;
		expected = "AZ";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 60;
		expected = "BH";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 705;
		expected = "AAC";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));

		column = 4136;
		expected = "FCB";
		actual = GevoTestExcelLoader.getColumn(column);
		assertThat(actual, is(equalTo(expected)));
	}

}
