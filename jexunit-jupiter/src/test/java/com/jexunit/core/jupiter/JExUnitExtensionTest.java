package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.dataprovider.ExcelFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.opentest4j.MultipleFailuresError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests running complete worksheets with the JUnit 5 extension. The (nested) test classes are run via the
 * {@link EngineTestKit} only.
 */
class JExUnitExtensionTest {

    @Test
    void shouldPassOnExpectedExceptions() {
        execute(ExpectedExceptionTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void shouldFailOnMissingExpectedException() {
        execute(MissingExpectedExceptionTest.class).assertStatistics(stats -> stats.started(1).failed(1));
    }

    @Test
    void shouldStartEachWorksheetWithNewTestContext() {
        execute(TestContextResetTest.class).assertStatistics(stats -> stats.started(2).succeeded(2));
    }

    @Test
    void shouldKeepCollectedFailuresOnMissingCommandImplementation() {
        final Events events = execute(MissingImplementationTest.class);

        events.assertStatistics(stats -> stats.started(1).failed(1));
        final Throwable failure = events.failed().list().get(0)
                .getPayload(TestExecutionResult.class).flatMap(TestExecutionResult::getThrowable)
                .orElseThrow(IllegalStateException::new);
        assertInstanceOf(MultipleFailuresError.class, failure);
        // the failed assertion before and the missing implementation
        assertEquals(2, ((MultipleFailuresError) failure).getFailures().size());
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    @ExtendWith(JExUnitExtension.class)
    public static class ExpectedExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "exception"},
                    {"throwException", "true"},
                    {"failAssertion", "true"}});
        }

        @ExcelTest
        void test() {
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

    @ExtendWith(JExUnitExtension.class)
    public static class MissingImplementationTest {

        @ExcelFile
        public static String excelFile() {
            // the second command is implemented for another test class only, so it passes the validation
            return ExcelTestFile.create(new String[][]{
                    {"command"},
                    {"failAssertion"},
                    {"otherTestClassCommand"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("failAssertion")
        public static void failAssertion() {
            fail("expected");
        }
    }

    /**
     * Not run, only provides a command implementation for this test class.
     */
    public static class OtherTestClass {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{{"command"}, {"otherTestClassCommand"}});
        }

        @TestCommand("otherTestClassCommand")
        public static void otherTestClassCommand() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class MissingExpectedExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "exception"},
                    {"doNothing", "true"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("doNothing")
        public static void doNothing() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class TestContextResetTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{{"command"}, {"store"}, {"assertBeforeEach"}},
                    new String[][]{{"command"}, {"assertNotStored"}, {"assertBeforeEach"}});
        }

        @BeforeEach
        void prepareContext() {
            TestContextManager.add("beforeEach", "from @BeforeEach");
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("store")
        public static void store(final TestContext context) {
            context.add("value", "from the first worksheet");
        }

        @TestCommand("assertNotStored")
        public static void assertNotStored(final TestContext context) {
            assertNull(context.get(String.class, "value"));
        }

        @TestCommand("assertBeforeEach")
        public static void assertBeforeEach(final TestContext context) {
            // values added in @BeforeEach are not removed by the reset
            assertEquals("from @BeforeEach", context.get(String.class, "beforeEach"));
        }
    }

}
