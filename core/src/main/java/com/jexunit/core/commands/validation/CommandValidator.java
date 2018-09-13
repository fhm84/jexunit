package com.jexunit.core.commands.validation;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.TestCommandScanner;
import com.jexunit.core.model.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

public class CommandValidator {

    private static final Logger log = Logger.getLogger(CommandValidator.class.getName());

    /**
     * Validates test cases after they are parsed
     *
     * @param testData test data (loaded by a data provider)
     */
    public static void validateCommands(final Collection<Object[]> testData) {
        final ValidationType validationType = ValidationType
                .valueOf(JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_VALIDATION_TYPE));
        if (validationType == ValidationType.IGNORE) {
            return;
        }
        for (final Object[] objects : testData) {
            final List<TestCase<?>> testCases = (List<TestCase<?>>) objects[0];
            final Iterator<TestCase<?>> iterator = testCases.iterator();
            while (iterator.hasNext()) {
                final TestCase<?> testCase = iterator.next();
                if (!TestCommandScanner.isTestCommandValid(testCase.getTestCommand().toLowerCase())) {
                    if (validationType == ValidationType.WARN) {
                        log.log(Level.WARNING, "TestCommand {0} is not valid. TestCase will be removed! {1} {2}",
                                new String[]{testCase.getTestCommand(),
                                        testCase.getMetadata().getDetailedIdentifier(), testCase.getComment()});
                        iterator.remove();
                    } else if (validationType == ValidationType.FAIL) {
                        fail(String.format("TestCommand %s is not valid.", testCase.getTestCommand()));
                    }
                }
            }
        }
    }

}
