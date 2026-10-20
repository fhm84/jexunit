package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import org.junit.After;
import org.junit.Test;

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

}
