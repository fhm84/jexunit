package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.dataprovider.ExcelFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for commands with several implementations (overloaded methods). The (nested) test classes are run via the
 * {@link EngineTestKit} only.
 */
class OverloadedCommandTest {

    @Test
    void shouldSelectImplementationByColumns() {
        OverloadTest.CALLS.clear();

        execute(OverloadTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
        assertEquals(Arrays.asList("name: John", "name: Jane, age: 42", "name: Max"), OverloadTest.CALLS);
    }

    @Test
    void shouldFailOnAmbiguousOrUnmatchedImplementations() {
        final Events events = execute(AmbiguousOverloadTest.class);

        events.assertStatistics(stats -> stats.started(3).failed(3));
        final List<String> messages = events.failed().list().stream()
                .map(event -> event.getPayload(TestExecutionResult.class).flatMap(TestExecutionResult::getThrowable)
                        .orElseThrow(IllegalStateException::new).getMessage())
                .collect(Collectors.toList());
        assertTrue(messages.get(0).contains("Test command 'ambiguousOverload' is ambiguous"), messages.get(0));
        assertTrue(messages.get(0).contains("ambiguousOverload(@TestParam(\"first\") String)"), messages.get(0));
        assertTrue(messages.get(0).contains("ambiguousOverload(@TestParam(\"second\") String, TestContext)"),
                messages.get(0));
        assertTrue(messages.get(1).contains(
                "No implementation of test command 'ambiguousOverload' has values for all its required parameters"),
                messages.get(1));
        // an ambiguous command never runs, so it doesn't satisfy an expected exception
        assertTrue(messages.get(2).contains("Test command 'ambiguousOverload' is ambiguous"), messages.get(2));
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    @ExtendWith(JExUnitExtension.class)
    public static class OverloadTest {

        static final List<String> CALLS = new ArrayList<>();

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "name", "age"},
                    {"overloaded", "John"},
                    {"overloaded", "Jane", "42"},
                    // an empty cell is no value
                    {"overloaded", "Max", ""}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("overloaded")
        public static void overloaded(@TestParam("name") final String name) {
            CALLS.add("name: " + name);
        }

        @TestCommand("overloaded")
        public static void overloaded(@TestParam("name") final String name, @TestParam("age") final int age) {
            CALLS.add("name: " + name + ", age: " + age);
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class AmbiguousOverloadTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    // both implementations have a value for one parameter
                    new String[][]{{"command", "first", "second"}, {"ambiguousOverload", "a", "b"}},
                    // no implementation has a value for its required parameter
                    new String[][]{{"command", "third"}, {"ambiguousOverload", "c"}},
                    new String[][]{{"command", "first", "second", "exception"}, {"ambiguousOverload", "a", "b", "true"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("ambiguousOverload")
        public static void ambiguousOverload(@TestParam("first") final String first) {
        }

        @TestCommand("ambiguousOverload")
        public static void ambiguousOverload(@TestParam("second") final String second, final TestContext context) {
        }
    }
}
