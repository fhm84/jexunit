package com.jexunit.examples.features;

import com.jexunit.core.JExUnit;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

@RunWith(JExUnit.class)
public class MultilineTest {

	@ExcelFile
	static String[] excelFiles = new String[]{"src/test/resources/MultilineTest.xlsx"};
	private static List<Map<String, TestCell>> multilineValues;
	private static List<Map<String, TestCell>> singleLineValues;
	private static Map<String, TestCell> singleLineValue;
	private static List<Map<String, TestCell>> defaultMultilineValues;


	@TestCommand("createPerson")
	public static void createPerson(TestCase<?> testCase) {
		if (testCase.isMultiline()) {
			multilineValues = testCase.getMultilineValues();
		} else {
			singleLineValues = testCase.getMultilineValues();
			singleLineValue = testCase.getValues();
		}
	}

	@TestCommand("createPersonMultiline")
	public static void createPersonMultiline(TestCase<?> testCase) {
		Assert.assertTrue(testCase.isMultiline());
		defaultMultilineValues = testCase.getMultilineValues();
	}

	@Test
	public void testMultilineValues() {
		// Drei Multiline-Zeilen hintereinander
		Assert.assertEquals(3, multilineValues.size());


		// Sortierung wie im Excel
		Assert.assertEquals("Max", multilineValues.get(0).get("firstname").getValue());
		Assert.assertEquals("Manfred", multilineValues.get(1).get("firstname").getValue());
		Assert.assertEquals("Rudi", multilineValues.get(2).get("firstname").getValue());

		// Multiline-Spalte selbst ist nicht aufgeführt
		Assert.assertEquals(4, multilineValues.get(0).values().size());
		Assert.assertEquals(4, multilineValues.get(1).values().size());
		Assert.assertEquals(4, multilineValues.get(2).values().size());

	}

	@Test
	public void testDefaultMultilineValues() {
		// Drei Multiline-Zeilen hintereinander
		Assert.assertEquals(3, defaultMultilineValues.size());


		// Sortierung wie im Excel
		Assert.assertEquals("Robert", defaultMultilineValues.get(0).get("firstname").getValue());
		Assert.assertEquals("Simon", defaultMultilineValues.get(1).get("firstname").getValue());
		Assert.assertEquals("Julian", defaultMultilineValues.get(2).get("firstname").getValue());

		// Multiline-Spalte selbst ist nicht aufgeführt
		Assert.assertEquals(3, defaultMultilineValues.get(0).values().size());
		Assert.assertEquals(3, defaultMultilineValues.get(1).values().size());
		Assert.assertEquals(3, defaultMultilineValues.get(2).values().size());

	}

	@Test
	public void testSinglelineValues() {
		// Multiline-Zugriff auch bei Single-Line möglich
		Assert.assertEquals(1, singleLineValues.size());

		// Letzter Eintrag ist der aktuelle
		Assert.assertEquals("Roberta", singleLineValues.get(0).get("firstname").getValue());

		// Multiline-Zeiger zeigt auf aktuellen Wert
		Assert.assertEquals(singleLineValue, singleLineValues.get(0));

		// Multiline-Spalte selbst ist nicht aufgeführt
		Assert.assertEquals(4, multilineValues.get(0).values().size());
	}


}
