package com.jexunit.core.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class will invoke the test command. This will also be used as factory to be able to add proxies for auditing,
 * logging, statistics and so on.
 *
 * @author Fabian
 */
public class TestCommandInvocationHandler implements Invocable {

    public static Invocable getInvocationHandler(final Command testCommand, final Method method, final Object o) {
        // TODO: here we can "decide" which proxies to use ...
        final TestCommandInvocationHandler tcih = new TestCommandInvocationHandler(testCommand, method, o);

        // ServiceLoader<InvocationHandler> invocationHandlers = ServiceLoader.load(InvocationHandler.class);
        // invocationHandlers.
        // for () {
        //
        // }
        // return AuditProxy.newInstance(tcih);
        return tcih;
    }

    Command testCommand;
    Method method;
    Object o;

    public TestCommandInvocationHandler(final Command testCommand, final Method method, final Object o) {
        this.testCommand = testCommand;
        this.method = method;
        this.o = o;
    }

    @Override
    public void invoke(final Object... parameters) throws Exception {
        try {
            method.invoke(o, parameters);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
            // the method could not be called (e.g. no value for a primitive parameter), so the command didn't run
            throw new TestCommandSetupError(String.format("Test command method %s.%s could not be invoked: %s",
                    method.getDeclaringClass().getSimpleName(), method.getName(), e), e);
        } catch (final InvocationTargetException e) {
            // throw what the command threw (not the reflection wrapper), so the runners can tell assertion failures,
            // assumptions (skipping the test) and other exceptions apart
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }

}
