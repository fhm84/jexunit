package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.model.TestCase;
import com.jexunit.examples.arithmeticaltests.model.ArithmeticalTestObject;

/**
 * Provide the Test-Commands for the Arithmetical tests.
 * 
 * @author fabian
 * 
 */
public class ArithmeticalTestCommands {

	private static final Logger log = Logger.getLogger(ArithmeticalTestCommands.class.getName());

	public static void runCommand(TestCase<?> testCase) throws Exception {
		Double val1, val2, result;
		val1 = Double.parseDouble(testCase.getValues().get("param1").getValue());
		val2 = Double.parseDouble(testCase.getValues().get("param2").getValue());
		result = Double.parseDouble(testCase.getValues().get("result").getValue());
		log.log(Level.INFO, "run command (testCase: {0})", testCase);
		switch (testCase.getTestCommand()) {
		case "ADD":
			assertThat(val1 + val2, equalTo(result));
			break;
		case "SUB":
		case "SUBTRACT":
			assertThat(val1 - val2, equalTo(result));
			break;
		case "MUL":
			assertThat(val1 * val2, equalTo(result));
			break;
		case "DIV":
			assertThat(val1 / val2, equalTo(result));
			break;
		}
	}

	public static void runCommandWithObject(TestCase<?> testCase) throws Exception {
		ArithmeticalTestObject obj = TestObjectHelper.createObject(testCase, ArithmeticalTestObject.class);
		log.log(Level.INFO, "run comand with object (testCase: {0})", testCase);
		switch (testCase.getTestCommand()) {
		case "ADD":
			assertThat(obj.getParam1() + obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "SUB":
		case "SUBTRACT":
			assertThat(obj.getParam1() - obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "MUL":
			assertThat(obj.getParam1() * obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "DIV":
			assertThat(obj.getParam1() / obj.getParam2(), equalTo(obj.getResult()));
			break;
		}
	}

	@TestCommand("add")
	public void runAddCommand(TestCase<?> testCase) throws Exception {
		log.info("in test command: ADD!");
		ArithmeticalTestObject obj = TestObjectHelper.createObject(testCase, ArithmeticalTestObject.class);
		assertThat(obj.getParam1() + obj.getParam2(), equalTo(obj.getResult()));
	}

	@TestCommand({ "sub", "subtract" })
	public void runSubCommand(TestCase<?> testCase, ArithmeticalTestObject testObject) throws Exception {
		log.info("in test command: " + testCase.getTestCommand() + "!");
		assertThat(testObject.getParam1() - testObject.getParam2(), equalTo(testObject.getResult()));
	}
}
