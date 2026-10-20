package com.jexunit.core;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.dataprovider.ExcelFile;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests running complete worksheets with the JUnit 4 runner. The (nested) test classes are not run by surefire itself.
 */
public class JExUnitTest {

    @Test
    public void shouldPassOnExpectedExceptions() {
        final Result result = JUnitCore.runClasses(ExpectedExceptionTest.class);

        assertEquals(1, result.getRunCount());
        assertEquals(String.valueOf(result.getFailures()), 0, result.getFailureCount());
    }

    @Test
    public void shouldFailOnMissingExpectedException() {
        final Result result = JUnitCore.runClasses(MissingExpectedExceptionTest.class);

        assertEquals(1, result.getRunCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    public void shouldStartEachWorksheetWithNewTestContext() {
        final Result result = JUnitCore.runClasses(TestContextResetTest.class);

        assertEquals(2, result.getRunCount());
        assertEquals(String.valueOf(result.getFailures()), 0, result.getFailureCount());
    }

    @Test
    public void shouldReportOriginalFailureOnFastFail() {
        final Result result = JUnitCore.runClasses(FastFailTest.class);

        assertEquals(1, result.getRunCount());
        // the second row is not executed after the fast fail
        assertEquals(1, result.getFailureCount());
        assertThat(result.getFailures().get(0).getMessage(),
                containsString("No Exception expected in TestCommand: failAssertion"));
    }

    @Test
    public void shouldContinueAfterUnexpectedExceptionsAndErrors() {
        final Result result = JUnitCore.runClasses(UnexpectedExceptionTest.class);

        assertEquals(1, result.getRunCount());
        assertEquals(String.valueOf(result.getFailures()), 3, result.getFailureCount());
    }

    @Test
    public void shouldSkipDisabledWorksheet() {
        final Result result = JUnitCore.runClasses(DisabledWorksheetTest.class);

        assertEquals(1, result.getRunCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getAssumptionFailureCount());
    }

    @Test
    public void shouldRunAllExcelFilesAndOwnCommandsWhenExtendingJExUnitBase() {
        InheritedTest.INVOCATIONS.set(0);

        final Result result = JUnitCore.runClasses(InheritedTest.class);

        assertEquals(2, result.getRunCount());
        assertEquals(String.valueOf(result.getFailures()), 0, result.getFailureCount());
        assertEquals(2, InheritedTest.INVOCATIONS.get());
    }

    @RunWith(JExUnit.class)
    public static class FastFailTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "fastFail"},
                    {"failAssertion", "true"},
                    {"failAssertion", "true"}});
        }

        @TestCommand("failAssertion")
        public static void failAssertion() {
            fail("expected");
        }
    }

    @RunWith(JExUnit.class)
    public static class UnexpectedExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command"},
                    {"throwException"},
                    {"throwError"},
                    {"failAssertion"}});
        }

        @TestCommand("throwException")
        public static void throwException() {
            throw new IllegalStateException("unexpected");
        }

        @TestCommand("throwError")
        public static void throwError() {
            throw new LinkageError("unexpected");
        }

        @TestCommand("failAssertion")
        public static void failAssertion() {
            fail("expected");
        }
    }

    @RunWith(JExUnit.class)
    public static class DisabledWorksheetTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"disabled", "true"},
                    {"command"},
                    {"failAssertion"}});
        }

        @TestCommand("failAssertion")
        public static void failAssertion() {
            fail("must not be executed");
        }
    }

    public static class InheritedTest extends JExUnitBase {

        static final AtomicInteger INVOCATIONS = new AtomicInteger();

        @ExcelFile
        public static String[] excelFiles() {
            return new String[]{
                    ExcelTestFile.create(new String[][]{{"command"}, {"countInvocation"}}),
                    ExcelTestFile.create(new String[][]{{"command"}, {"countInvocation"}})};
        }

        @TestCommand("countInvocation")
        public void countInvocation() {
            INVOCATIONS.incrementAndGet();
        }
    }

    @RunWith(JExUnit.class)
    public static class ExpectedExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "exception"},
                    {"throwException", "true"},
                    {"failAssertion", "true"}});
        }

        @TestCommand("throwException")
        public static void throwException() {
            throw new IllegalStateException("expected");
        }

        @TestCommand("failAssertion")
        public static void failAssertion() {
            fail("expected");
        }
    }

    @RunWith(JExUnit.class)
    public static class MissingExpectedExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "exception"},
                    {"doNothing", "true"}});
        }

        @TestCommand("doNothing")
        public static void doNothing() {
        }
    }

    @RunWith(JExUnit.class)
    public static class TestContextResetTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{{"command"}, {"store"}},
                    new String[][]{{"command"}, {"assertNotStored"}});
        }

        @TestCommand("store")
        public static void store(final TestContext context) {
            context.add("value", "from the first worksheet");
        }

        @TestCommand("assertNotStored")
        public static void assertNotStored(final TestContext context) {
            assertNull(context.get(String.class, "value"));
        }
    }

}
