package com.jexunit.core.jupiter;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.commands.TestCommandRunner;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertAll;

class JExUnitInterceptor implements InvocationInterceptor {

    private static final Logger log = Logger.getLogger(JExUnitInterceptor.class.getName());

    private final List<TestCase<?>> testCases;
    private final String identifier;

    JExUnitInterceptor(final List<TestCase<?>> testCases, final String identifier) {
        this.testCases = testCases;
        this.identifier = identifier;
    }

    @Override
    public void interceptTestTemplateMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {

        invocation.skip();

        if (testCases == null || testCases.isEmpty()) {
            return;
        }

        final Object testInstance = extensionContext.getRequiredTestInstance();
        final JUnit5TestCommandHost host = new JUnit5TestCommandHost(testInstance);
        final TestCommandRunner runner = new TestCommandRunner(host);

        log.log(Level.INFO, "Running TestCase: {0}", testCases.get(0).getMetadata().getTestGroup());

        final List<Throwable> errors = new ArrayList<>();
        boolean abort = false;

        outerLoop:
        for (final TestCase<?> testCase : testCases) {
            if (abort) break;
            final boolean exceptionExpected = testCase.isExceptionExpected();
            try {
                final String cmd = testCase.getTestCommand();
                if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED).equalsIgnoreCase(cmd)) {
                    if (testCase.isDisabled()) {
                        log.info(String.format("Testsheet disabled! (%s)", testCase.getMetadata().getDetailedIdentifier()));
                        throw new TestAbortedException("Testsheet disabled: " + testCase.getMetadata().getDetailedIdentifier());
                    }
                } else if (JExUnitConfig.getDefaultCommandProperty(DefaultCommands.REPORT).equalsIgnoreCase(cmd)) {
                    for (final TestCell tc : testCase.getValues().values()) {
                        log.info(tc.getValue());
                    }
                    continue;
                } else {
                    if (testCase.isDisabled()) {
                        log.info(String.format("Testcase disabled! (command: %s, %s) %s",
                                cmd, testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()));
                        continue;
                    }
                    try {
                        runner.runTestCommand(testCase);
                    } catch (final TestAbortedException e) {
                        throw e;
                    } catch (final AssertionError e) {
                        if (!exceptionExpected) {
                            errors.add(new AssertionError(String.format(
                                    "No Exception expected in TestCommand: %s, %s. %s",
                                    cmd, testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()), e));
                            if (isFastFail(testCase)) { abort = true; continue outerLoop; }
                        }
                        continue;
                    } catch (final Exception e) {
                        boolean handled = false;
                        Throwable t = e;
                        while ((t = t.getCause()) != null) {
                            if (t instanceof AssertionError) {
                                if (!exceptionExpected) {
                                    errors.add(new AssertionError(String.format(
                                            "No Exception expected in TestCommand: %s, %s. %s",
                                            cmd, testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()), t));
                                    if (isFastFail(testCase)) { abort = true; continue outerLoop; }
                                }
                                handled = true;
                                continue outerLoop;
                            }
                        }
                        if (!handled) {
                            e.printStackTrace();
                            errors.add(new AssertionError(String.format(
                                    "Unexpected Exception in TestCommand: %s, %s. (Exception: %s) %s",
                                    cmd, testCase.getMetadata().getDetailedIdentifier(), e, testCase.getComment()), e));
                            if (isFastFail(testCase)) { abort = true; continue outerLoop; }
                        }
                        continue;
                    }
                }

                if (exceptionExpected) {
                    errors.add(new AssertionError(String.format(
                            "Exception expected! in TestCommand: %s, %s. %s",
                            cmd, testCase.getMetadata().getDetailedIdentifier(), testCase.getComment())));
                    if (isFastFail(testCase)) { abort = true; }
                }

            } catch (final TestAbortedException e) {
                throw e;
            } catch (final Exception e) {
                log.log(Level.WARNING, "TestException", e);
                if (!exceptionExpected) {
                    errors.add(new AssertionError(String.format(
                            "Unexpected Exception (%s)! in TestCommand: %s, %s. %s",
                            e, testCase.getTestCommand(), testCase.getMetadata().getDetailedIdentifier(),
                            testCase.getComment()), e));
                }
            }
        }

        if (!errors.isEmpty()) {
            assertAll(errors.stream().map(e -> (org.junit.jupiter.api.function.Executable) () -> { throw e; }));
        }
    }

    private boolean isFastFail(final TestCase<?> testCase) {
        return Boolean.TRUE.equals(testCase.getFastFail());
    }
}
