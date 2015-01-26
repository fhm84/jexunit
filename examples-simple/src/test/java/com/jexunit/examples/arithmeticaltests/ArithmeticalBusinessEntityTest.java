package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitBase;
import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.commands.TestCommands;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.examples.arithmeticaltests.model.CustomTestObject;

/**
 * Simple Test for the framework.
 * <p>
 * This test doesn't extend the base-class ({@link JExUnitBase}). This test works with the
 * <code>@RunWith(GevoTester.class)</code>-Annotation as the integration point for the framework.
 * </p>
 * <p>
 * This test should provide all the arithmetical operations ADD, SUB, MUL and DIV. In this version
 * the automatic object-creation / -matching mechanism is tested. The test-command-methods get the
 * testCase and a model-object as parameters. The model-objects will automatically be created by the
 * framework. Therefore the parameter-names in the excel-file will be matched to the attribute-names
 * of the model-class. This will test the construction of (model-)object trees, to separate the
 * "test-entities" from the business-entities.
 * </p>
 * 
 * @author fabian
 * 
 */
@RunWith(JExUnit.class)
public class ArithmeticalBusinessEntityTest {

	private static Logger log = Logger.getLogger(ArithmeticalBusinessEntityTest.class.getName());

	@ExcelFile
	static String excelFile = "src/test/resources/ArithmeticalBusinessEntityTests.xlsx";

	@TestCommand(value = "add")
	public static void runAddCommand(CustomTestObject testObject) throws Exception {
		log.log(Level.INFO, "in test command: ADD!");
		assertThat(testObject.getEntity().getParam1() + testObject.getEntity().getParam2(),
				equalTo(testObject.getResult()));
	}

	@TestCommand(value = "sub")
	public static void runSubCommand(CustomTestObject testObject) throws Exception {
		log.log(Level.INFO, "in test command: SUB!");
		assertThat(testObject.getEntity().getParam1() - testObject.getEntity().getParam2(),
				equalTo(testObject.getResult()));
	}

	@TestCommand(value = "multiply")
	@TestCommand(value = "mul")
	public static void runMulCommand(CustomTestObject testObject) throws Exception {
		log.log(Level.INFO, "in test command: MUL!");
		assertThat(testObject.getEntity().getParam1() * testObject.getEntity().getParam2(),
				equalTo(testObject.getResult()));
	}

	@TestCommands({ @TestCommand(value = "divide"), @TestCommand(value = "div") })
	public static void runDivCommand(CustomTestObject testObject) throws Exception {
		log.log(Level.INFO, "in test command: DIV!");
		assertThat(testObject.getEntity().getParam1() / testObject.getEntity().getParam2(),
				equalTo(testObject.getResult()));
	}

}