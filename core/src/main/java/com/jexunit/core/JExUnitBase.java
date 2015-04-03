package com.jexunit.core;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.commands.TestCommandRunner;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.junit.Parameterized;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import com.jexunit.core.spi.data.DataProvider;

/**
 * BaseClass for the Tests (BusinessTransactionTests or simple unit-tests).<br>
 * This class is used to read the excel-file and "create" the separated junit-tests for each worksheet. Each test will
 * get a list of test-cases containing comands to execute.<br>
 * <br>
 * To run your own test-command, you have to implement the {@link #runCommand(TestCase)}-method. All the surrounding
 * features are implemented in this BaseClass. So you can define commands like "disable" the test-case, "report"
 * something and "expect" an exception. The only thing you have to do is to implement your own commands!
 * 
 * @author fabian
 * 
 *         TODO: check the file for valid commands while reading/parsing it?
 * 
 */
@Ignore
@RunWith(Parameterized.class)
public class JExUnitBase {

	private static Logger log = Logger.getLogger(JExUnitBase.class.getName());

	@Rule
	public ErrorCollector errorCollector = new ErrorCollector();

	@Parameter
	List<TestCase> testCases;

	/**
	 * Get the type of the running test-class to identify, if a test-command is provided by the test-class itself or by
	 * another class. If the {@link #getClass()}-Method returns the JExUnitBase, this method will return null. This
	 * indicates the framework, that the test-class was executed with the {@link @RunWith}-Annotation (so, the
	 * JExUnit-class).
	 */
	private Class<?> testType = null;

	private TestCommandRunner testCommandRunner;

	public JExUnitBase() {
		JExUnitConfig.init();
		testCommandRunner = new TestCommandRunner(this);
	}

	public void setTestType(Class<?> testType) {
		this.testType = testType;
	}

	public Class<?> getTestType() {
		return testType;
	}

	/**
	 * Returns collection of input data for each test run.
	 * 
	 * @param excelFile
	 *            the name/path of the excel file to load the test data from
	 * @param worksheetAsTest
	 *            "group" all the test-commands of a worksheet to one test (true) or run each test-command as single
	 *            test (false)
	 * 
	 * @return the parameters for the {@link Parameterized}-JUnit-TestSuite
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	@Parameters(name = "{0} [{index}]")
	public static Collection<Object[]> setUp(int testNumber) throws Exception {
		DataProvider dataProvider = TestContextManager.get(DataProvider.class);
		return dataProvider.loadTestData(testNumber);
	}

	/**
	 * This is the test-method for junit. Here the iteration through the {@link TestCase}s and
	 * interpretation/implementation of the test-commands will run.
	 */
	@Test
	public void test() {
		if (testCases == null || testCases.isEmpty()) {
			return;
		}

		log.log(Level.INFO, "Running TestCase: {0}", testCases.get(0).getSheet());
		testCaseLoop: for (TestCase testCase : testCases) {
			boolean exceptionExpected = testCase.isExceptionExpected();
			try {
				if (JExUnitConfig.getStringProperty(DefaultCommands.DISABLED.getConfigKey()).equalsIgnoreCase(
						testCase.getTestCommand())) {
					if (testCase.isDisabled()) {
						log.info(String.format("Testcase disabled! (Worksheet: %s)", testCase.getSheet()));
						// if the test is disabled, ignore the junit-test (assume will pass the
						// test)
						Assume.assumeTrue(String.format("Testcase disabled! (Worksheet: %s)", testCase.getSheet()),
								true);
						return;
					}
				} else if (JExUnitConfig.getStringProperty(DefaultCommands.REPORT.getConfigKey()).equalsIgnoreCase(
						testCase.getTestCommand())) {
					// log all the report-"values"
					for (TestCell tc : testCase.getValues().values()) {
						log.info(tc.getValue());
					}
					// continue: there is nothing else to do; you cannot expect an exception on a
					// "report"-command
					continue testCaseLoop;
				} else {
					try {
						// run the test-command
						testCommandRunner.runTestCommand(testCase);
					} catch (AssertionError e) {
						if (!exceptionExpected) {
							errorCollector.addError(new AssertionError(String.format(
									"No Exception expected in TestCommand: %s, worksheet: %s, row: %s",
									testCase.getTestCommand(), testCase.getSheet(), testCase.getRow()), e));
						} else {
							continue testCaseLoop;
						}
					} catch (Exception e) {
						Throwable t = e;
						while ((t = t.getCause()) != null) {
							if (t instanceof AssertionError) {
								if (!exceptionExpected) {
									errorCollector.addError(new AssertionError(String.format(
											"No Exception expected in TestCommand: %s, worksheet: %s, row: %s",
											testCase.getTestCommand(), testCase.getSheet(), testCase.getRow()), t));
								} else {
									continue testCaseLoop;
								}
							}
						}
						e.printStackTrace();
						fail(String
								.format("Unexpected Exception thrown in TestCommand: %s, worksheet: %s, row: %s. (Exception: %s)",
										testCase.getTestCommand(), testCase.getSheet(), testCase.getRow(), e));
					}
				}

				// if an exception is expected, but no exception is thrown, the test will fail!
				if (exceptionExpected) {
					errorCollector.addError(new AssertionError(String.format(
							"Exception expected! in TestCommand: %s, worksheet: %s, row: %s",
							testCase.getTestCommand(), testCase.getSheet(), testCase.getRow())));
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "TestException", e);
				if (!exceptionExpected) {
					fail(String.format("Unexpected Exception thrown (%s)! in TestCommand: %s, worksheet: %s, row: %s",
							e, testCase.getTestCommand(), testCase.getSheet(), testCase.getRow()));
				}
			}
		}
	}

	/**
	 * This method runs your specified Test-Command. In the {@link TestCase} you will find all information you need
	 * (read from the excel file/row) to run the command.<br>
	 * You have to implement this method to run your specific tests.
	 * 
	 * @param testCase
	 *            the TestCase containing all information from the excel file/row
	 * 
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	public void runCommand(TestCase testCase) throws Exception {
		errorCollector
				.addError(new NoSuchMethodError(
						String.format(
								"No implementation found for the command \"%1$s\". Please override this method in your Unit-Test or provide a method annotated with @TestCommand(\"%1$s\")",
								testCase.getTestCommand())));
	}
}
