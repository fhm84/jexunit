package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.dataprovider.ExcelFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for errors while preparing a command (before it runs): they never satisfy an expected exception. The (nested)
 * test classes are run via the {@link EngineTestKit} only.
 */
class SetupErrorTest {

    @Test
    void shouldNotSatisfyExpectedExceptionWithSetupErrors() {
        SetupErrorsTest.calls = 0;
        final Events events = execute(SetupErrorsTest.class);

        events.assertStatistics(stats -> stats.started(4).failed(4));
        final List<String> messages = events.failed().list().stream()
                .map(event -> event.getPayload(TestExecutionResult.class).flatMap(TestExecutionResult::getThrowable)
                        .orElseThrow(IllegalStateException::new).getMessage())
                .collect(Collectors.toList());
        assertTrue(messages.get(0).contains("Required parameter not found: value"), messages.get(0));
        assertTrue(messages.get(1).contains("Test command 'setupNumber' could not be prepared"), messages.get(1));
        assertTrue(messages.get(2).contains("only property access is supported"), messages.get(2));
        assertTrue(messages.get(3).contains("Test command method SetupErrorsTest.setupPrimitive could not be invoked"), messages.get(3));
        // none of the commands ran
        assertEquals(0, SetupErrorsTest.calls);
    }

    @Test
    void shouldStillSatisfyExpectedExceptionWithExceptionOfCommand() {
        execute(CommandExceptionTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void shouldSatisfyExpectedExceptionWithExceptionOfModel() {
        final Events events = execute(ModelExceptionTest.class);

        // the validating setter throws: expected in the first worksheet, unexpected in the second one
        events.assertStatistics(stats -> stats.started(2).succeeded(1).failed(1));
        final String message = events.failed().list().get(0).getPayload(TestExecutionResult.class)
                .flatMap(TestExecutionResult::getThrowable).orElseThrow(IllegalStateException::new).getMessage();
        assertTrue(message.contains("Unexpected Exception in TestCommand: validate"), message);
        assertTrue(message.contains("age must not be negative"), message);
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    @ExtendWith(JExUnitExtension.class)
    public static class SetupErrorsTest {

        static int calls;

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    // a required parameter without value
                    new String[][]{{"command", "exception"}, {"setupRequired", "true"}},
                    // a value that cannot be converted
                    new String[][]{{"command", "number", "exception"}, {"setupNumber", "abc", "true"}},
                    // a header rejected by the property access restriction
                    new String[][]{{"command", "name.toUpperCase()", "exception"}, {"setupEntity", "John", "true"}},
                    // no value for an optional primitive parameter: the method cannot be called
                    new String[][]{{"command", "exception"}, {"setupPrimitive", "true"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("setupRequired")
        public static void setupRequired(@TestParam("value") final String value) {
            calls++;
        }

        @TestCommand("setupNumber")
        public static void setupNumber(@TestParam("number") final int number) {
            calls++;
        }

        @TestCommand("setupEntity")
        public static void setupEntity(final Person person) {
            calls++;
        }

        @TestCommand("setupPrimitive")
        public static void setupPrimitive(@TestParam(value = "number", required = false) final int number) {
            calls++;
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class CommandExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{{"command", "name", "exception"}, {"throwing", "John", "true"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("throwing")
        public static void throwing(@TestParam("name") final String name) {
            throw new IllegalArgumentException("thrown by the command");
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class ModelExceptionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{{"command", "age", "exception"}, {"validate", "-1", "true"}},
                    new String[][]{{"command", "age"}, {"validate", "-1"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("validate")
        public static void validate(final ValidatingPerson person) {
        }
    }

    public static class ValidatingPerson {

        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            if (age < 0) {
                throw new IllegalArgumentException("age must not be negative");
            }
            this.age = age;
        }
    }

    public static class Person {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
