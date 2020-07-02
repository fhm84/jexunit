package com.jexunit.core;

import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.commands.TestCommandRunner;
import com.jexunit.core.commands.validation.CommandValidator;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.junit.Parameterized;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import com.jexunit.core.spi.AfterSheet;
import com.jexunit.core.spi.BeforeSheet;
import com.jexunit.core.spi.data.DataProvider;
import org.junit.*;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

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
 */
@Ignore
@RunWith(Parameterized.class)
public class JExUnitBase {

    private static final Logger log = Logger.getLogger(JExUnitBase.class.getName());

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Rule
    public TestWatcher watcher;

    @Parameter
    List<TestCase<?>> testCases;

    /**
     * Get the type of the running test-class to identify, if a test-command is provided by the test-class itself or by
     * another class. If the {@link #getClass()}-Method returns the JExUnitBase, this method will return null. This
     * indicates the framework, that the test-class was executed with the {@link @RunWith}-Annotation (so, the
     * JExUnit-class).
     */
    private Class<?> testType = null;

    private final TestCommandRunner testCommandRunner;

    public JExUnitBase() {
        JExUnitConfig.init();
        testCommandRunner = new TestCommandRunner(this);
    }

    public void setTestType(final Class<?> testType) {
        this.testType = testType;
    }

    public Class<?> getTestType() {
        return testType;
    }

    /**
     * Returns collection of input data for each test run.
     *
     * @param testNumber the number (identifier, index, ...) of the test
     * @return the parameters for the {@link Parameterized}-JUnit-TestSuite
     * @throws Exception in case that something goes wrong
     */
    @Parameters(name = "{0} [{index}]")
    public static Collection<Object[]> setUp(final int testNumber) throws Exception {
        final DataProvider dataProvider = TestContextManager.get(DataProvider.class);
        final Collection<Object[]> testData = dataProvider.loadTestData(testNumber);
        CommandValidator.validateCommands(testData);
        return testData;
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

        log.log(Level.INFO, "Running TestCase: {0}", testCases.get(0).getMetadata().getTestGroup());
        // FIXME: fastFail only current TestGroup?
        testCaseLoop:
        for (final TestCase<?> testCase : testCases) {
            final boolean exceptionExpected = testCase.isExceptionExpected();
            try {
                if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED)
                        .equalsIgnoreCase(testCase.getTestCommand())) {
                    if (testCase.isDisabled()) {
                        log.info(String.format("Testsheet disabled! (%s)",
                                testCase.getMetadata().getDetailedIdentifier()));
                        // if the testsheet is disabled, ignore the junit-test (assume will pass the
                        // test)
                        Assume.assumeTrue(String.format("Testsheet disabled! (%s)",
                                testCase.getMetadata().getDetailedIdentifier()), true);
                        return;
                    }
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.REPORT)
                        .equalsIgnoreCase(testCase.getTestCommand())) {
                    // log all the report-"values"
                    for (final TestCell tc : testCase.getValues().values()) {
                        log.info(tc.getValue());
                    }
                    // continue: there is nothing else to do; you cannot expect an exception on a
                    // "report"-command
                    continue testCaseLoop;
                } else {
                    try {
                        if (testCase.isDisabled()) {
                            log.info(String.format("Testcase disabled! (command: %s, %s) %s", testCase.getTestCommand(),
                                    testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()));
                            // if the testCase is disabled, ignore it (assume will pass the test)
                            Assume.assumeTrue(
                                    String.format("Testcase disabled! (command: %s, %s) %s", testCase.getTestCommand(),
                                            testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()),
                                    true);
                            continue testCaseLoop;
                        }
                        // run the test-command
                        testCommandRunner.runTestCommand(testCase);
                    } catch (final AssertionError e) {
                        if (!exceptionExpected) {
                            errorCollector.addError(new AssertionError(String.format(
                                    "No Exception expected in TestCommand: %s, %s. %s", testCase.getTestCommand(),
                                    testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()), e));
                            if (testCase.isFastFail()) {
                                fail("FastFail attribute forces the complete test sheet to fail.");
                            }
                        } else {
                            continue testCaseLoop;
                        }
                    } catch (final Exception e) {
                        Throwable t = e;
                        while ((t = t.getCause()) != null) {
                            if (t instanceof AssertionError) {
                                if (!exceptionExpected) {
                                    errorCollector.addError(new AssertionError(String.format(
                                            "No Exception expected in TestCommand: %s, %s. %s",
                                            testCase.getTestCommand(), testCase.getMetadata().getDetailedIdentifier(),
                                            testCase.getComment()), t));
                                    if (testCase.isFastFail()) {
                                        fail("FastFail attribute forces the complete test sheet to fail.");
                                    }
                                } else {
                                    continue testCaseLoop;
                                }
                            }
                        }
                        e.printStackTrace();
                        fail(String.format("Unexpected Exception thrown in TestCommand: %s, %s. (Exception: %s) %s",
                                testCase.getTestCommand(), testCase.getMetadata().getDetailedIdentifier(), e,
                                testCase.getComment()));
                    }
                }

                // if an exception is expected, but no exception is thrown, the test will fail!
                if (exceptionExpected) {
                    errorCollector.addError(new AssertionError(
                            String.format("Exception expected! in TestCommand: %s, %s. %s", testCase.getTestCommand(),
                                    testCase.getMetadata().getDetailedIdentifier(), testCase.getComment())));

                    if (testCase.isFastFail()) {
                        log.log(Level.FINE, "FastFail activated");
                        fail("FastFail attribute forces the complete test sheet to fail.");
                        return;
                    }
                }
            } catch (final Exception e) {
                log.log(Level.WARNING, "TestException", e);
                if (!exceptionExpected) {
                    fail(String.format("Unexpected Exception thrown (%s)! in TestCommand: %s, %s. %s", e,
                            testCase.getTestCommand(), testCase.getMetadata().getDetailedIdentifier(),
                            testCase.getComment()));
                }
            }
        }
    }

    /**
     * This method runs your specified Test-Command. In the {@link TestCase} you will find all information you need
     * (read from the excel file/row) to run the command.<br>
     * You have to implement this method to run your specific tests.
     *
     * @param testCase the TestCase containing all information from the excel file/row
     * @throws Exception in case that something goes wrong
     */
    public void runCommand(final TestCase<?> testCase) throws Exception {
        errorCollector.addError(new NoSuchMethodError(String.format(
                "No implementation found for the command \"%1$s\". "
                        + "Please override this method in your Unit-Test or provide a method or class annotated with " +
                        "@TestCommand(\"%1$s\")",
                testCase.getTestCommand())));
    }

    @BeforeClass
    public static void initialseTest() throws Exception {
        String clazzName = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.BEFORE_EXCEL);
        if (clazzName != null && !clazzName.isEmpty()) {
            Class<?> name = Class.forName(clazzName);
            if (BeforeSheet.class.isAssignableFrom(name)) {
                BeforeSheet instance = (BeforeSheet) name.getDeclaredConstructor().newInstance();
                instance.run();
            }
        }
    }

    @AfterClass
    public static void finalizeTest() throws Exception{
        String clazzName = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.AFTER_EXCEL);
        if (clazzName != null && !clazzName.isEmpty()) {
            Class<?> name = Class.forName(clazzName);
            if (AfterSheet.class.isAssignableFrom(name)) {
                AfterSheet instance = (AfterSheet) name.getDeclaredConstructor().newInstance();
                instance.run();
            }
        }

    }

}
