package com.jexunit.core.jupiter;

import com.jexunit.core.commands.annotation.TestCommand;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for the built-in <code>verify</code> command. The (nested) test classes are run via the {@link EngineTestKit}
 * only.
 */
class VerifyCommandTest {

    @Test
    void shouldPassIfAllValuesMatch() {
        execute(MatchingTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
    }

    @Test
    void shouldReportAllMismatchesOfTheRowsInOneFailure() {
        final List<String> messages = failureMessages(execute(MismatchTest.class));

        // the worksheet with exception = true passes: a failed verification is an assertion error
        assertEquals(1, messages.size());
        final String message = messages.get(0);
        assertTrue(message.contains("Verification failed (3 mismatches)"), message);
        assertTrue(message.contains("order.customer.name (C4): expected \"Müller\" but was \"Miller\""), message);
        assertTrue(message.contains("order.status (D4): expected \"CLOSED\" but was \"OPEN\""), message);
        // second row of the multiline command, verifying the same object
        assertTrue(message.contains("order.items[0].qty (E5): expected \"4\" but was \"3\""), message);
    }

    @Test
    void shouldFailIfTheObjectCannotBeFound() {
        final List<String> messages = failureMessages(execute(UnknownObjectTest.class));

        assertEquals(3, messages.size());
        assertTrue(messages.get(0).contains("Nothing was added to the TestContext with the id 'invoice'"),
                messages.get(0));
        // the command could not be prepared, so it doesn't satisfy an expected exception
        assertTrue(messages.get(1).contains("Nothing was added to the TestContext with the id 'invoice'"),
                messages.get(1));
        assertTrue(messages.get(2).contains("the column 'object' with the id of the object to verify"),
                messages.get(2));
    }

    @Test
    void shouldPreferCommandImplementationOverBuiltInVerify() {
        OwnVerifyCommandTest.CALLS.clear();

        execute(OwnVerifyCommandTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));
        assertEquals(1, OwnVerifyCommandTest.CALLS.size());
    }

    private static Events execute(final Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(testClass)).execute().testEvents();
    }

    /**
     * Get the messages of the failed tests, each including the messages of the collected failures and their causes
     * (the runner reports the failure of a command with the original failure as cause).
     */
    private static List<String> failureMessages(final Events events) {
        return events.failed().list().stream()
                .map(event -> messages(event.getPayload(TestExecutionResult.class)
                        .flatMap(TestExecutionResult::getThrowable).orElseThrow(IllegalStateException::new)))
                .collect(Collectors.toList());
    }

    private static String messages(final Throwable throwable) {
        final StringBuilder sb = new StringBuilder();
        final List<Throwable> failures = throwable instanceof MultipleFailuresError
                ? ((MultipleFailuresError) throwable).getFailures() : Collections.singletonList(throwable);
        for (final Throwable failure : failures) {
            for (Throwable t = failure; t != null; t = t.getCause()) {
                sb.append(t.getMessage()).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public enum Status {
        OPEN, CLOSED
    }

    public static class Customer {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Item {

        private int id;
        private int qty;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(final int qty) {
            this.qty = qty;
        }
    }

    public static class Order {

        private int id = 42;
        private Customer customer = new Customer();
        private Status status = Status.OPEN;
        private BigDecimal total = new BigDecimal("12.50");
        private String note;
        private List<Item> items = new ArrayList<>();

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(final Customer customer) {
            this.customer = customer;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(final Status status) {
            this.status = status;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(final BigDecimal total) {
            this.total = total;
        }

        public String getNote() {
            return note;
        }

        public void setNote(final String note) {
            this.note = note;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(final List<Item> items) {
            this.items = items;
        }
    }

    /**
     * Commands creating the order to verify (outside of the test classes, so all of them can use it).
     */
    public static class OrderCommands {

        @TestCommand("verifyCreateOrder")
        public static void createOrder(final TestContext context) {
            final Order order = new Order();
            order.getCustomer().setName("Miller");
            final Item item = new Item();
            item.setId(5);
            item.setQty(3);
            order.getItems().add(item);
            context.add("order", order);
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class MatchingTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command"},
                    {"verifyCreateOrder"},
                    {"command", "expectedQty"},
                    {"set", "3"},
                    {"command", "object", "id", "customer.name", "status", "total", "note", "items[id=5].qty",
                            "items[0].id"},
                    // BigDecimal with another scale, null value, a variable as expected value, empty = not checked
                    {"verify", "order", "42", "Miller", "OPEN", "12.5", "<null>", "${expectedQty}", ""},
                    {"command", "object", "name"},
                    // a property path as object
                    {"verify", "order.customer", "Miller"}});
        }

        @ExcelTest
        void test() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class MismatchTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{
                            {"command"},
                            {"verifyCreateOrder"},
                            {"command", "object", "customer.name", "status", "items[0].qty", "multiline"},
                            {"verify", "order", "Müller", "CLOSED", "", "true"},
                            {"verify", null, "Miller", "", "4"}},
                    new String[][]{
                            {"command"},
                            {"verifyCreateOrder"},
                            {"command", "object", "status", "exception"},
                            {"verify", "order", "CLOSED", "true"}});
        }

        @ExcelTest
        void test() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class UnknownObjectTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(
                    new String[][]{{"command", "object", "id"}, {"verify", "invoice", "1"}},
                    new String[][]{{"command", "object", "id", "exception"}, {"verify", "invoice", "1", "true"}},
                    new String[][]{{"command", "id"}, {"verify", "1"}});
        }

        @ExcelTest
        void test() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class OwnVerifyCommandTest {

        static final List<String> CALLS = new ArrayList<>();

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{{"command", "object"}, {"verify", "unknown"}});
        }

        @ExcelTest
        void test() {
        }

        @TestCommand("verify")
        public static void verify() {
            CALLS.add("own verify");
        }
    }
}
