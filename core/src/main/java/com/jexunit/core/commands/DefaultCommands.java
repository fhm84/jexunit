package com.jexunit.core.commands;

import lombok.Getter;

/**
 * Here the default/built-in commands and command-attributes are defined. These are the following ones: disabled,
 * exception_expected, report, breakpoint, comment.
 *
 * @author fabian
 */
@Getter
public enum DefaultCommands {

    /**
     * Default command/parameter for disabling a "test"-worksheet.
     */
    DISABLED("jexunit.defaultcommand.disabled", "disabled"),

    /**
     * Default command for reporting (log something from the excel-file).
     */
    REPORT("jexunit.defaultcommand.report", "report"),

    /**
     * Parameter for expecting an exception.
     */
    EXCEPTION_EXCPECTED("jexunit.defaultcommand.exception_expected", "exception"),

    /**
     * Parameter for a comment in case the assertion/command fails. This parameter can be used like the message for
     * asserts.
     */
    COMMENT("jexunit.defaultcommand.comment", "comment"),

    /**
     * Parameter for a command to fast fail the complete test group. This parameter will override the default setting of
     * the test command.
     */
    FAST_FAIL("jexunit.defaultcommand.fastfail", "fastFail"),

    /**
     * Parameter for a multiline command. This parameter will override the default setting of
     * the test command.
     */
    MULTILINE("jexunit.defaultcommand.multiline", "multiline"),

    /**
     * Parameter for "setting a breakpoint inside the excel-file" to be able to debug the test more easily.
     */
    BREAKPOINT("jexunit.defaultcommand.breakpoint", "breakpoint");

    private final String configKey;
    private final String defaultValue;

    DefaultCommands(final String configKey, final String defaultValue) {
        this.configKey = configKey;
        this.defaultValue = defaultValue;
    }

}
