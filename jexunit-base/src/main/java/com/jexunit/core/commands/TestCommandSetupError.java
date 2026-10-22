package com.jexunit.core.commands;

/**
 * A test command could not be prepared, so it did not run: e.g. no implementation matches the row, a required
 * parameter has no value, a value cannot be converted, or a column header cannot be mapped to the entity.
 * <p>
 * It is an error (not an exception), so it never satisfies an expected exception (<code>exception = true</code>):
 * only exceptions thrown by the command itself (or by the user's model while the row is mapped, e.g. by a setter
 * validating its value) do.
 *
 * @author fabian
 */
public class TestCommandSetupError extends Error {

    private static final long serialVersionUID = 1L;

    public TestCommandSetupError(final String message) {
        super(message);
    }

    public TestCommandSetupError(final String message, final Throwable cause) {
        super(message, cause);
    }

}
