package com.jexunit.core.jupiter;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.spi.CommandInstanceFactory;
import com.jexunit.core.spi.DefaultCommandInstanceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.opentest4j.MultipleFailuresError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for creating the command classes and command providers with a {@link CommandInstanceFactory}. The (nested)
 * test classes are run via the {@link EngineTestKit} only.
 */
class CommandInstanceFactoryTest {

    private static final String FACTORY_KEY = JExUnitConfig.ConfigKey.COMMAND_INSTANCE_FACTORY.getKey();

    @AfterEach
    void resetConfig() {
        JExUnitConfig.setConfigProperty(FACTORY_KEY, JExUnitConfig.ConfigKey.COMMAND_INSTANCE_FACTORY.getDefaultConfig());
    }

    @Test
    void shouldCreateCommandInstancesWithConfiguredFactory() {
        JExUnitConfig.setConfigProperty(FACTORY_KEY, GreeterFactory.class.getName());
        GreeterFactory.CREATED.clear();
        CALLS.clear();

        execute(FactoryTest.class).assertStatistics(stats -> stats.started(1).succeeded(1));

        // a command class for each invocation, a command provider once per test group
        assertEquals(Arrays.asList(
                "GreetCommand (ExtensionContext: true, test instance: FactoryTest)",
                "GreetingProvider (ExtensionContext: true, test instance: FactoryTest)",
                "GreetCommand (ExtensionContext: true, test instance: FactoryTest)"), GreeterFactory.CREATED);
        assertEquals(Arrays.asList("Hello John", "provided: Hello Jane", "Hello Max", "provided: Hello Eve"), CALLS);
    }

    @Test
    void shouldReportWhyTheDefaultFactoryCannotCreateTheInstance() {
        final List<String> messages = failureMessages(execute(DefaultFactoryTest.class));

        assertEquals(2, messages.size());
        // no no-arg constructor
        assertTrue(messages.get(0).contains("Test command 'factoryGreet' could not be prepared: "
                + "java.lang.NoSuchMethodException"), messages.get(0));
        // the exception of the constructor itself (not an InvocationTargetException)
        assertTrue(messages.get(1).contains("Test command 'factoryFailingConstructor' could not be prepared: "
                + "java.lang.IllegalStateException: constructor failed"), messages.get(1));
    }

    @Test
    void shouldFailOnInvalidFactory() {
        JExUnitConfig.setConfigProperty(FACTORY_KEY, String.class.getName());

        final List<String> messages = failureMessages(execute(FactoryTest.class));

        // each row fails
        assertEquals(4, messages.size());
        assertTrue(messages.get(0).contains("java.lang.String (configured in jexunit.command.instance_factory) is no "
                + CommandInstanceFactory.class.getName()), messages.get(0));
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

    static final List<String> CALLS = new ArrayList<>();

    /**
     * A "service" injected into the commands.
     */
    public static class Greeter {

        private final String greeting;

        public Greeter(final String greeting) {
            this.greeting = greeting;
        }

        String greet(final String name) {
            return greeting + " " + name;
        }
    }

    /**
     * Factory "injecting" the greeter, like a dependency injection container.
     */
    public static class GreeterFactory implements CommandInstanceFactory {

        static final List<String> CREATED = new ArrayList<>();

        private final Greeter greeter = new Greeter("Hello");

        @Override
        public <T> T create(final Class<T> type, final Context context) throws Exception {
            CREATED.add(String.format("%s (ExtensionContext: %s, test instance: %s)", type.getSimpleName(),
                    context.unwrap(ExtensionContext.class).isPresent(),
                    context.getTestInstance().getClass().getSimpleName()));
            if (type == GreetCommand.class) {
                return type.cast(new GreetCommand(greeter));
            }
            final T instance = new DefaultCommandInstanceFactory().create(type, context);
            if (instance instanceof GreetingProvider) {
                ((GreetingProvider) instance).setGreeter(greeter);
            }
            return instance;
        }
    }

    /**
     * A command class with a dependency (no no-arg constructor).
     */
    @TestCommand("factoryGreet")
    public static class GreetCommand {

        private final Greeter greeter;

        public GreetCommand(final Greeter greeter) {
            this.greeter = greeter;
        }

        public void run(@TestParam("name") final String name) {
            CALLS.add(greeter.greet(name));
        }
    }

    @TestCommand("factoryFailingConstructor")
    public static class FailingConstructorCommand {

        public FailingConstructorCommand() {
            throw new IllegalStateException("constructor failed");
        }

        public void run() {
        }
    }

    /**
     * A command provider with a dependency (set by the factory).
     */
    public static class GreetingProvider {

        private Greeter greeter;

        void setGreeter(final Greeter greeter) {
            this.greeter = greeter;
        }

        @TestCommand("factoryProvided")
        public void provided(@TestParam("name") final String name) {
            CALLS.add("provided: " + greeter.greet(name));
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class FactoryTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "name"},
                    {"factoryGreet", "John"},
                    {"factoryProvided", "Jane"},
                    {"factoryGreet", "Max"},
                    {"factoryProvided", "Eve"}});
        }

        @ExcelTest
        void test() {
        }
    }

    @ExtendWith(JExUnitExtension.class)
    public static class DefaultFactoryTest {

        @ExcelFile
        public static String excelFile() {
            return ExcelTestFile.create(new String[][]{
                    {"command", "name"},
                    {"factoryGreet", "John"},
                    {"factoryFailingConstructor"}});
        }

        @ExcelTest
        void test() {
        }
    }
}
