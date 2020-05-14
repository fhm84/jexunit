package com.jexunit.examples.features;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@RunWith(JExUnit.class)
public class FormattingTest {

	@ExcelFile(worksheetAsTest = false)
	static String[] excelFiles = new String[]{"src/test/resources/FormattingTest.xlsx"};

	@TestCommand("map")
	public static void runMap(TestCase<?> testCase, HashMap<String, String> params) throws Exception {
		String param1 = params.get("param1");
		String param2 = params.get("param2");

		// Random Int generator is evaluated exactly once
		Assert.assertEquals(param1, param2);

		// Map key without value is truncated
		Assert.assertFalse(params.containsKey("param3"));

		// param3 as well as empty date formatted cell in G6 is ignored
		Assert.assertEquals(2, params.size());
	}

	@TestCommand("COMPAREDATE")
	public static void run(HashMap<String, String> params) {
		String date = params.get("date");
		// Same date than today preserving configured date pattern
		String format = new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATE_PATTERN))
				.format(new Date());

		Assert.assertEquals(format, date);

		date = params.get("timestamp");
		// Same date than today preserving configured date pattern
		format = new SimpleDateFormat(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.DATETIME_PATTERN))
				.format(new Date());
		// Compare two timestamps (From Excel and Java). Because there is a little time-difference (maximum 1 second) between these dates
		Assert.assertEquals(format.substring(0, format.length() - 2), date.substring(0, date.length() - 2));
	}

	@TestCommand("COMPAREINT")
	public static void compareInt(HashMap<String, String> params) {
		String integer = params.get("integer");
		Assert.assertEquals("1", integer);

		String aFloat = params.get("float");
		Assert.assertEquals("1.999", aFloat);
	}
}


