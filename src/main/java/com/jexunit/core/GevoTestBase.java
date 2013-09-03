package com.jexunit.core;

import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.jexunit.core.commands.TestCommandMethodScanner;
import com.jexunit.core.junit.Parameterized;

/**
 * BaseClass for the GevoTests (BusinessTransactionTests).<br>
 * This class is used to read the excel-file and "create" the separated junit-tests for each
 * worksheet. Each test will get a list of test-cases containing comands to execute.<br>
 * <br>
 * To run your own test-command, you have to implement the {@link #runCommand(GevoTestCase)}-method.
 * All the surrounding features are implemented in this BaseClass. So you can define commands like
 * "disable" the test-case, "report" something and "expect" an exception. The only thing you have to
 * do is to implement your own commands!
 * 
 * @author fabian
 * 
 *         TODO: add possibility to override the default commands?
 * 
 *         TODO: check the file for valid commands while reading/parsing it?
 * 
 *         TODO: use instances of CommandProviders (not only static command-methods for holding
 *         state in the command-providers)?
 * 
 */
@RunWith(Parameterized.class)
public class GevoTestBase {

	private static Logger log = Logger.getLogger(GevoTestBase.class.getName());

	public static final String EXCEPTION_EXCPECTED = "exception";

	@Parameter
	List<GevoTestCase> testCases;

	/**
	 * Get the type of the running test-class to identify, if a test-command is provided by the
	 * test-class itself or by another class. If the {@link #getClass()}-Method returns the
	 * GevoTestBase, this method will return null. This indicates the framework, that the test-class
	 * was executed with the {@link @RunWith}-Annotation (so, the GevoTester-class).
	 */
	private Class<?> testType = null;

	public GevoTestBase() {
	}

	public void setTestType(Class<?> testType) {
		this.testType = testType;
	}

	/**
	 * Returns collection of input data for each test run.
	 * 
	 * @param excelFile
	 *            the name/path of the excel file to load the test data from
	 * @param worksheetAsTest
	 *            "group" all the test-commands of a worksheet to one test (true) or run each
	 *            test-command as single test (false)
	 * 
	 * @return the parameters for the {@link Parameterized}-JUnit-TestSuite
	 */
	@Parameters(name = "{0} [{index}]")
	public static Collection<Object[]> setUp(String excelFile, boolean worksheetAsTest)
			throws Exception {
		return GevoTestExcelLoader.loadTestData(excelFile, worksheetAsTest);
	}

	/**
	 * This is the test-method for junit. Here the iteration through the {@link GevoTestCase}s and
	 * interpretation/implementation of the test-commands will run.
	 */
	@Test
	public void test() {
		if (testCases == null || testCases.isEmpty()) {
			return;
		}

		log.log(Level.INFO, "Running GevoTestCase: {0}", testCases.get(0).getSheet());
		testCaseLoop: for (GevoTestCase testCase : testCases) {
			boolean exceptionExpected = false;
			try {
				// each command has the ability to expect an exception. you can define this via the
				// field EXCEPTION_EXPECTED.
				GevoTestCell exceptionCell = testCase.getValues().get(EXCEPTION_EXCPECTED);
				if (exceptionCell != null) {
					exceptionExpected = Boolean.parseBoolean(exceptionCell.getValue());
				}

				if (GevoTestExcelLoader.DISABLED.equalsIgnoreCase(testCase.getTestCommand())) {
					if (Boolean.parseBoolean(testCase.getValues().get(GevoTestExcelLoader.DISABLED)
							.getValue())) {
						log.info(String.format("Testcase disabled! (Worksheet: %s)",
								testCase.getSheet()));
						// if the test is disabled, ignore the junit-test (assume will pass the
						// test)
						Assume.assumeTrue(
								String.format("Testcase disabled! (Worksheet: %s)",
										testCase.getSheet()), true);
						return;
					}
				} else if (GevoTestExcelLoader.REPORT.equalsIgnoreCase(testCase.getTestCommand())) {
					// log all the report-"values"
					for (GevoTestCell tc : testCase.getValues().values()) {
						log.info(tc.getValue());
					}
					// continue: there is nothing else to do; you cannot expect an exception on a
					// "report"-command
					continue testCaseLoop;
				} else {
					try {
						runTestCommand(testCase);
					} catch (AssertionError e) {
						if (!exceptionExpected) {
							fail(String
									.format("Exception expected! in GevoTestCommand: %s, worksheet: %s, row: %s",
											testCase.getTestCommand(), testCase.getSheet(),
											testCase.getRow()));
						} else {
							continue testCaseLoop;
						}
					} catch (Exception e) {
						Throwable t = e;
						while ((t = t.getCause()) != null) {
							if (t instanceof AssertionError) {
								if (!exceptionExpected) {
									fail(String
											.format("Exception expected! in GevoTestCommand: %s, worksheet: %s, row: %s",
													testCase.getTestCommand(), testCase.getSheet(),
													testCase.getRow()));
								} else {
									continue testCaseLoop;
								}
							}
						}
						e.printStackTrace();
						fail(String
								.format("Unexpected Exception thrown in GevoTestCommand: %s, worksheet: %s, row: %s. (Exception: %s)",
										testCase.getTestCommand(), testCase.getSheet(),
										testCase.getRow(), e));
					}
				}

				// if an exception is expected, but no exception is thrown, the test will fail!
				if (exceptionExpected) {
					fail(String.format(
							"Exception expected! in GevoTestCommand: %s, worksheet: %s, row: %s",
							testCase.getTestCommand(), testCase.getSheet(), testCase.getRow()));
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "GevoTestException", e);
				if (!exceptionExpected) {
					fail(String
							.format("Unexpected Exception thrown (%s)! in GevoTestCommand: %s, worksheet: %s, row: %s",
									e, testCase.getTestCommand(), testCase.getSheet(),
									testCase.getRow()));
				}
			}
		}
	}

	/**
	 * Get the method for the current testCommand (via the {@code @TestCommand}-Annotation) and call
	 * it.
	 * 
	 * @param testCase
	 *            the current testCase to run
	 * 
	 * @throws Exception
	 */
	private void runTestCommand(GevoTestCase testCase) throws Exception {
		// check, which method to run for the current GevoTestCommand
		Method method = TestCommandMethodScanner.getTestCommandMethod(testCase.getTestCommand()
				.toLowerCase(), testType);
		if (method != null) {
			if (method.getParameterTypes().length == 1) {
				if (method.getDeclaringClass() == this.getClass()) {
					method.invoke(this, testCase);
				} else {
					method.invoke(null, testCase);
				}
			} else if (method.getParameterTypes().length == 2) {
				Class<?> type = method.getParameterTypes()[1];
				Object o = GevoTestObjectHelper.createObject(testCase, type);
				try {
					if (method.getDeclaringClass() == this.getClass()) {
						method.invoke(this, testCase, o);
					} else {
						method.invoke(null, testCase, o);
					}
				} catch (IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					Throwable t = e;
					while (t.getCause() != null) {
						t = t.getCause();
					}
					if (t instanceof AssertionError) {
						throw (AssertionError) t;
					}
					throw e;
				}
			} else {

			}
		} else {
			runCommand(testCase);
		}
	}

	/**
	 * This method runs your specified Test-Command. In the {@link GevoTestCase} you will find all
	 * information you need (read from the excel file/row) to run the command.<br>
	 * You have to implement this method to run your specific tests.
	 * 
	 * @param testCase
	 *            the GevoTestCase containing all information from the excel file/row
	 * 
	 * @throws Exception
	 */
	public void runCommand(GevoTestCase testCase) throws Exception {
		throw new NoSuchMethodError(
				String.format(
						"No implementation found for the command \"%1$s\". Please override this method in your Unit-Test or provide a method annotated with @TestCommand(\"%1$s\")",
						testCase.getTestCommand()));
	}
}
