package com.jexunit.core.commands;

import lombok.Getter;

/**
 * Here the default/built-in commands and command-attributes are defined. These are the following ones: disabled,
 * exception_expected, report, set, comment, fastFail, multiline, breakpoint.
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
     * Default command for setting variables: each column is a variable (the header is its name), added to the
     * TestContext with the value of the cell. A command implementation with the same name takes precedence.
     */
    SET("jexunit.defaultcommand.set", "set", false),

    /**
     * Parameter for expecting an exception.
     */
    EXCEPTION_EXPECTED("jexunit.defaultcommand.exception_expected", "exception"),

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
     * Parameter for a list of multiline commands. This parameter will override the default setting of
     * the test command.
     */
    MULTILINE_COMMANDS("jexunit.multiline_commands", ""),

    /**
     * Parameter for "setting a breakpoint inside the excel-file" to be able to debug the test more easily.
     */
    BREAKPOINT("jexunit.defaultcommand.breakpoint", "breakpoint");

    private final String configKey;
    private final String defaultValue;
    /**
     * Whether the name is (also) used as a column, so the column is removed from the values passed to the commands. The
     * commands added later (like set) are not, so they don't hide columns of the user's model with the same name.
     */
    private final boolean column;

    DefaultCommands(final String configKey, final String defaultValue) {
        this(configKey, defaultValue, true);
    }

    DefaultCommands(final String configKey, final String defaultValue, final boolean column) {
        this.configKey = configKey;
        this.defaultValue = defaultValue;
        this.column = column;
    }

}
