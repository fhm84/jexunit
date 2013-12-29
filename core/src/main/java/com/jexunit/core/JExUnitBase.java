package com.jexunit.core;

import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.commands.TestCommandMethodScanner;
import com.jexunit.core.commands.TestParam;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.data.ExcelLoader;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.junit.Parameterized;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

/**
 * BaseClass for the Tests (BusinessTransactionTests or simple unit-tests).<br>
 * This class is used to read the excel-file and "create" the separated junit-tests for each
 * worksheet. Each test will get a list of test-cases containing comands to execute.<br>
 * <br>
 * To run your own test-command, you have to implement the {@link #runCommand(TestCase)}-method. All
 * the surrounding features are implemented in this BaseClass. So you can define commands like
 * "disable" the test-case, "report" something and "expect" an exception. The only thing you have to
 * do is to implement your own commands!
 * 
 * @author fabian
 * 
 *         TODO: add possibility to override the default commands?
 * 
 *         TODO: check the file for valid commands while reading/parsing it?
 * 
 */
@RunWith(Parameterized.class)
public class JExUnitBase {

	private static Logger log = Logger.getLogger(JExUnitBase.class.getName());

	@Rule
	public ErrorCollector errorCollector = new ErrorCollector();

	@Parameter
	List<TestCase> testCases;

	/**
	 * Get the type of the running test-class to identify, if a test-command is provided by the
	 * test-class itself or by another class. If the {@link #getClass()}-Method returns the
	 * JExUnitBase, this method will return null. This indicates the framework, that the test-class
	 * was executed with the {@link @RunWith}-Annotation (so, the JExUnit-class).
	 */
	private Class<?> testType = null;

	public JExUnitBase() {
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
		return ExcelLoader.loadTestData(excelFile, worksheetAsTest);
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
			boolean exceptionExpected = false;
			try {
				// each command has the ability to expect an exception. you can define this via the
				// field EXCEPTION_EXPECTED.
				TestCell exceptionCell = testCase.getValues().get(
						DefaultCommands.EXCEPTION_EXCPECTED.getCommandName());
				if (exceptionCell != null) {
					exceptionExpected = Boolean.parseBoolean(exceptionCell.getValue());
				}

				if (DefaultCommands.DISABLED.getCommandName().equalsIgnoreCase(
						testCase.getTestCommand())) {
					if (Boolean.parseBoolean(testCase.getValues()
							.get(DefaultCommands.DISABLED.getCommandName()).getValue())) {
						log.info(String.format("Testcase disabled! (Worksheet: %s)",
								testCase.getSheet()));
						// if the test is disabled, ignore the junit-test (assume will pass the
						// test)
						Assume.assumeTrue(
								String.format("Testcase disabled! (Worksheet: %s)",
										testCase.getSheet()), true);
						return;
					}
				} else if (DefaultCommands.REPORT.getCommandName().equalsIgnoreCase(
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
						// remove the parameters used by the framework
						removeFrameworkParameters(testCase);
						// run the test-command
						runTestCommand(testCase);
					} catch (AssertionError e) {
						if (!exceptionExpected) {
							errorCollector
									.addError(new AssertionError(
											String.format(
													"No Exception expected in TestCommand: %s, worksheet: %s, row: %s",
													testCase.getTestCommand(), testCase.getSheet(),
													testCase.getRow()), e));
						} else {
							continue testCaseLoop;
						}
					} catch (Exception e) {
						Throwable t = e;
						while ((t = t.getCause()) != null) {
							if (t instanceof AssertionError) {
								if (!exceptionExpected) {
									errorCollector
											.addError(new AssertionError(
													String.format(
															"No Exception expected in TestCommand: %s, worksheet: %s, row: %s",
															testCase.getTestCommand(),
															testCase.getSheet(), testCase.getRow()),
													t));
								} else {
									continue testCaseLoop;
								}
							}
						}
						e.printStackTrace();
						fail(String
								.format("Unexpected Exception thrown in TestCommand: %s, worksheet: %s, row: %s. (Exception: %s)",
										testCase.getTestCommand(), testCase.getSheet(),
										testCase.getRow(), e));
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
					fail(String
							.format("Unexpected Exception thrown (%s)! in TestCommand: %s, worksheet: %s, row: %s",
									e, testCase.getTestCommand(), testCase.getSheet(),
									testCase.getRow()));
				}
			}
		}
	}

	/**
	 * Remove the parameters used by the framework to only pass the "users" parameters to the
	 * commands.
	 * 
	 * @param testCase
	 *            current TestCase
	 */
	private void removeFrameworkParameters(TestCase testCase) {
		for (DefaultCommands dc : DefaultCommands.values()) {
			testCase.getValues().remove(dc.getCommandName());
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
	private void runTestCommand(TestCase testCase) throws Exception {
		// check, which method to run for the current TestCommand
		Method method = TestCommandMethodScanner.getTestCommandMethod(testCase.getTestCommand()
				.toLowerCase(), testType);
		if (method != null) {
			// prepare the parameters
			List<Object> parameters = prepareParameters(testCase, method);

			// invoke the method with the parameters
			invokeTestCommandMethod(method, parameters.toArray());
		} else {
			runCommand(testCase);
		}
	}

	/**
	 * Prepare the parameters for the given method (representing the test-command implementation)
	 * out of the given test-case.
	 * 
	 * @param testCase
	 *            the test-case to prepare the parameters from
	 * @param method
	 *            the method representing the test-command implementation
	 * @return the parameters-list to invoke the method with
	 * @throws Exception
	 */
	private List<Object> prepareParameters(TestCase testCase, Method method) throws Exception {
		List<Object> parameters = new ArrayList<>(method.getParameterTypes().length);
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int i = 0;
		for (Class<?> parameterType : method.getParameterTypes()) {
			if (parameterType == TestCase.class) {
				parameters.add(testCase);
			} else if (parameterType == TestContext.class) {
				parameters.add(TestContextManager.getTestContext());
			} else {
				if (parameterAnnotations[i].length > 0) {
					for (Annotation a : parameterAnnotations[i]) {
						if (a instanceof Context) {
							// add an instance out of the test-context
							Context ctx = (Context) a;
							String id = ctx.value();
							if (id == null || id.isEmpty()) {
								// lookup the instance out of the current TestContext
								parameters.add(TestContextManager.get(parameterType));
							} else {
								parameters.add(TestContextManager.get(parameterType, id));
							}
							break;
						} else if (a instanceof TestParam) {
							// add "single" test-param here
							TestParam param = (TestParam) a;
							String key = param.value();
							String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
							Object value = TestObjectHelper.convertPropertyStringToObject(
									parameterType, stringValue);
							if (param.required() && value == null) {
								throw new IllegalArgumentException("Required parameter not found: "
										+ key);
							}
							parameters.add(value);
						}
					}
				} else {
					Object o = TestObjectHelper.createObject(testCase, parameterType);
					parameters.add(o);
				}
			}
			i++;
		}
		return parameters;
	}

	/**
	 * Invoke the given method (representing the implementation of the test-command) with the given
	 * parameters. This will invoke the method static, on the current test-class or on the instance
	 * out of the test-context. If there is no instance in the test-context, a new instance will be
	 * created an put to the test-context.
	 * 
	 * @param method
	 *            the method to invoke
	 * @param parameters
	 *            the parameters for the method
	 * @throws Exception
	 */
	private void invokeTestCommandMethod(Method method, Object[] parameters) throws Exception {
		try {
			if (method.getDeclaringClass() == this.getClass()) {
				method.invoke(this, parameters);
			} else if (Modifier.isStatic(method.getModifiers())) {
				// invoke static
				method.invoke(null, parameters);
			} else {
				// create new instance of the Command-Class and put it to the test-context
				@SuppressWarnings("unchecked")
				Class<Object> clazz = (Class<Object>) method.getDeclaringClass();
				Object instance = TestContextManager.get(clazz);
				if (instance == null) {
					instance = clazz.newInstance();
					TestContextManager.add(clazz, instance);
				}
				method.invoke(instance, parameters);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			e.printStackTrace();
			throw e;
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
	}

	/**
	 * This method runs your specified Test-Command. In the {@link TestCase} you will find all
	 * information you need (read from the excel file/row) to run the command.<br>
	 * You have to implement this method to run your specific tests.
	 * 
	 * @param testCase
	 *            the TestCase containing all information from the excel file/row
	 * 
	 * @throws Exception
	 */
	public void runCommand(TestCase testCase) throws Exception {
		errorCollector
				.addError(new NoSuchMethodError(
						String.format(
								"No implementation found for the command \"%1$s\". Please override this method in your Unit-Test or provide a method annotated with @TestCommand(\"%1$s\")",
								testCase.getTestCommand())));
	}
}
