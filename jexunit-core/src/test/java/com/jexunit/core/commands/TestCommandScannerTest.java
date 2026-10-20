package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCommandScannerTest {

    private static final String PREFIX_KEY = JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getKey();
    private static final String REPORT_KEY = DefaultCommands.REPORT.getConfigKey();

    @After
    public void resetConfig() {
        JExUnitConfig.setConfigProperty(PREFIX_KEY, JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getDefaultConfig());
        JExUnitConfig.setConfigProperty(REPORT_KEY, DefaultCommands.REPORT.getDefaultValue());
    }

    @Test
    public void shouldRegisterCommandMethodsOfAClassOnlyOnce() {
        final List<LogRecord> warnings = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getLevel() == Level.WARNING) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final Logger log = Logger.getLogger(TestCommandScanner.class.getName());
        log.addHandler(handler);
        try {
            // the scan reports each annotated method of the class
            final TestCommandScanner scanner = new TestCommandScanner();
            scanner.reportMethodAnnotation(TestCommand.class, CommandFixture.class.getName(), "first");
            scanner.reportMethodAnnotation(TestCommand.class, CommandFixture.class.getName(), "second");
        } finally {
            log.removeHandler(handler);
        }

        assertEquals(Collections.emptyList(), warnings);
        assertTrue(TestCommandScanner.isTestCommandValid("scannerfixturefirst"));
        assertTrue(TestCommandScanner.isTestCommandValid("scannerfixturesecond"));
    }

    @Test
    public void shouldAcceptBuiltInCommands() {
        assertTrue(TestCommandScanner.isTestCommandValid("disabled"));
        assertTrue(TestCommandScanner.isTestCommandValid("report"));
        assertFalse(TestCommandScanner.isTestCommandValid("unknown"));
    }

    @Test
    public void shouldAcceptBuiltInCommandsWithConfiguredPrefix() {
        JExUnitConfig.setConfigProperty(PREFIX_KEY, "jx.");

        assertTrue(TestCommandScanner.isTestCommandValid("jx.disabled"));
        assertTrue(TestCommandScanner.isTestCommandValid("jx.report"));
        assertFalse(TestCommandScanner.isTestCommandValid("disabled"));
    }

    @Test
    public void shouldAcceptRenamedBuiltInCommands() {
        JExUnitConfig.setConfigProperty(REPORT_KEY, "Log");

        // command names are compared in lower case
        assertTrue(TestCommandScanner.isTestCommandValid("log"));
        assertFalse(TestCommandScanner.isTestCommandValid("report"));
    }

    public static class CommandFixture {

        @TestCommand("scannerFixtureFirst")
        public void first() {
        }

        @TestCommand("scannerFixtureSecond")
        public void second() {
        }
    }

}
