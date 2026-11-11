package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.dataprovider.ExcelFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.opentest4j.MultipleFailuresError;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for the injection of {@code @TestParam} and {@code @Context} fields of command classes. The (nested) test
 * classes are run via the {@link EngineTestKit} only.
 */
class FieldInjectionTest {

    static final List<String> CALLS = new ArrayList<>();

    @Test
    void shouldInjectFieldsOfCommandClasses() {
        CALLS.clear();

        execute(InjectionTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
        assertEquals(Arrays.asList(
                "customer: Miller, amount: 12.50, qty: 3, order: 42, label: express, count: 7",
                // a new instance for each invocation: the optional qty is not set (an empty cell is no value)
                "customer: Jones, amount: 1, qty: null, order: 42, label: express, count: 7",
                "b: second"), CALLS);
    }

    @Test
    void shouldFailOnMissingValues() {
        final List<String> messages = failureMessages(execute(MissingValueTest.class));

        assertEquals(4, messages.size());
        assertTrue(messages.get(0).contains("Required parameter not found: customer"), messages.get(0));
        // the command could not be prepared, so it doesn't satisfy an expected exception
        assertTrue(messages.get(1).contains("Required parameter not found: customer"), messages.get(1));
        assertTrue(messages.get(2).contains(
                "No value for count (the field PrimitiveFieldCommand.count of type int needs a value)"),
                messages.get(2));
        assertTrue(messages.get(3).contains("The field " + StaticFieldCommand.class.getName()
                + ".value cannot be injected: static and final fields are not supported"), messages.get(3));
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    /**
     * Get the messages of the failed tests, one per collected failure, each including the messages of its causes.
     */
    private static List<String> failureMessages(final Events events) {
        final List<String> messages = new ArrayList<>();
        events.failed().list().forEach(event -> {
            final Throwable throwable = event.getPayload(TestExecutionResult.class)
                    .flatMap(TestExecutionResult::getThrowable).orElseThrow(IllegalStateException::new);
            final List<Throwable> failures = throwable instanceof MultipleFailuresError
                    ? ((MultipleFailuresError) throwable).getFailures() : Collections.singletonList(throwable);
            for (final Throwable failure : failures) {
                final StringBuilder sb = new StringBuilder();
                for (Throwable t = failure; t != null; t = t.getCause()) {
                    sb.append(t.getMessage()).append(System.lineSeparator());
                }
                messages.add(sb.toString());
            }
        });
        return messages;
    }

    public static class Order {

        private final int id;

        public Order(final int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public abstract static class BaseCommand {

        // inherited field, the key is the name of the field
        @TestParam
        protected String customer;
    }

    @TestCommand("fieldInjection")
    public static class FieldCommand extends BaseCommand {

        @TestParam("amount")
        private BigDecimal amount;

        @TestParam(value = "qty", required = false)
        private Integer qty;

        @Context
        private Order order;

        @Context("label")
        private String label;

        // a primitive is looked up as its wrapper type
        @Context("count")
        private int count;

        public void run() {
            CALLS.add(String.format("customer: %s, amount: %s, qty: %s, order: %d, label: %s, count: %d", customer,
                    amount, qty, order.getId(), label, count));
        }
    }

    /**
     * Two command classes for the same command: the one with values for its fields is chosen.
     */
    @TestCommand("fieldOverload")
    public static class FirstOverloadCommand {

        @TestParam("a")
        private String a;

        public void run() {
            CALLS.add("a: " + a);
        }
    }

    @TestCommand("fieldOverload")
    public static class SecondOverloadCommand {

        @TestParam("b")
        private String b;

        public void run() {
            CALLS.add("b: " + b);
        }
    }

    @TestCommand("fieldPrimitive")
    public static class PrimitiveFieldCommand {

        @TestParam(value = "count", required = false)
        private int count;

        public void run() {
        }
    }

    @TestCommand("fieldStatic")
    public static class StaticFieldCommand {

        @TestParam("value")
        private static String value;

        public void run() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class InjectionTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command"},
                    {"fieldPrepareContext"},
                    {"command", "customer", "amount", "qty"},
                    {"fieldInjection", "Miller", "12.50", "3"},
                    {"fieldInjection", "Jones", "1", ""},
                    {"command", "b"},
                    {"fieldOverload", "second"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("fieldPrepareContext")
        public static void prepareContext(final TestContext context) {
            context.add(Order.class, new Order(42)).add("label", "express").add("count", 7);
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class MissingValueTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "amount", "exception"},
                    {"fieldInjection", "1"},
                    {"fieldInjection", "1", "true"},
                    {"command"},
                    {"fieldPrimitive"},
                    {"fieldStatic"}});
        }

        @ExcelTest
        void test() {
        }
    }
}
