package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for variables (<code>${name}</code>) in the cells and the built-in <code>set</code> command. The (nested) test
 * classes are run via the {@link EngineTestKit} only.
 */
class VariablesTest {

    @Test
    void shouldResolveVariablesWhenTheCommandRuns() {
        VariableTest.CALLS.clear();

        execute(VariableTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
        assertEquals(Arrays.asList(
                "addItem: order 42, qty 3",
                "addItem: order 42, qty 4",
                "entity: ORD-42 for Miller",
                "multiline: 42, 420"), VariableTest.CALLS);
    }

    @Test
    void shouldFailOnUnknownVariable() {
        final List<String> messages = failureMessages(execute(UnknownVariableTest.class));

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains("Unknown variable '${orderId}' in cell B2"), messages.get(0));
        // an unknown variable means the command could not be prepared, so it doesn't satisfy an expected exception
        assertTrue(messages.get(1).contains("Unknown variable '${orderId}' in cell B2"), messages.get(1));
    }

    @Test
    void shouldPreferCommandImplementationOverBuiltInSet() {
        OwnSetCommandTest.CALLS.clear();

        execute(OwnSetCommandTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
        assertEquals(Arrays.asList("own set: 1"), OwnSetCommandTest.CALLS);
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    private static List<String> failureMessages(final Events events) {
        return events.failed().list().stream()
                .map(event -> event.getPayload(TestExecutionResult.class).flatMap(TestExecutionResult::getThrowable)
                        .orElseThrow(IllegalStateException::new).getMessage())
                .collect(Collectors.toList());
    }

    public static class Order {

        private int id;
        private String customer;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getCustomer() {
            return customer;
        }

        public void setCustomer(final String customer) {
            this.customer = customer;
        }
    }

    public static class Label {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class VariableTest {

        static final List<String> CALLS = new ArrayList<>();

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "customer"},
                    {"varCreateOrder", "Miller"},
                    // a multiline block: the framework columns (comment) are removed from all rows
                    {"command", "prefix", "next", "comment", "multiline"},
                    {"set", "ORD", "", "the prefix", "true"},
                    {"set", "", "${order.id}"},
                    {"command"},
                    {"varNoCommentVariable"},
                    {"command", "order", "qty"},
                    {"varAddItem", "${order.id}", "3"},
                    // the value of a variable set in the sheet
                    {"varAddItem", "${next}", "4"},
                    {"report", "order ${order.id}, literal $${order.id}"},
                    {"command", "text"},
                    // mapped to an entity parameter
                    {"varEntity", "${prefix}-${order.id} for ${order.customer}"},
                    {"command", "value", "multiline"},
                    {"varMultiline", "${order.id}", "true"},
                    {"varMultiline", "${order.id}0"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("varCreateOrder")
        public static void createOrder(@TestParam("customer") final String customer, final TestContext context) {
            final Order order = new Order();
            order.setId(42);
            order.setCustomer(customer);
            context.add("order", order);
        }

        @TestCommand("varNoCommentVariable")
        public static void noCommentVariable(final TestContext context) {
            assertNull(context.get(Object.class, "comment"));
        }

        @TestCommand("varAddItem")
        public static void addItem(@TestParam("order") final int order, @TestParam("qty") final int qty) {
            CALLS.add("addItem: order " + order + ", qty " + qty);
        }

        @TestCommand("varEntity")
        public static void entity(final Label label) {
            CALLS.add("entity: " + label.getText());
        }

        @TestCommand("varMultiline")
        public static void multiline(final TestCase<?> testCase) {
            CALLS.add("multiline: " + testCase.getMultilineValues().stream()
                    .map(row -> row.get("value").getValue()).collect(Collectors.joining(", ")));
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class UnknownVariableTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{{"command", "order"}, {"varUnknown", "${orderId}"}},
                    new String[][]{{"command", "order", "exception"}, {"varUnknown", "${orderId}", "true"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("varUnknown")
        public static void unknown(@TestParam("order") final String order) {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class OwnSetCommandTest {

        static final List<String> CALLS = new ArrayList<>();

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{{"command", "value"}, {"set", "1"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("set")
        public static void set(@TestParam("value") final String value) {
            CALLS.add("own set: " + value);
        }
    }
}
