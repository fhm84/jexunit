package com.jexunit.core.commands;

/**
 * Interace for the TestCommandInvocationHandler to be able to "dynamic proxy" the TestCommandInvocationHandler to add
 * additional functionality like e.g. auditing, logging, statistics, ...
 *
 * @author Fabian
 */
public interface Invocable {

    /**
     * Invoke the command using the given parameters.
     *
     * @param parameters "current" parameters for invoking the test command
     * @throws Exception in case something went wrong
     */
    void invoke(final Object... parameters) throws Exception;

}
