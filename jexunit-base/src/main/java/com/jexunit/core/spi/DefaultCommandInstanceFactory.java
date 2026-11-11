package com.jexunit.core.spi;

import java.lang.reflect.InvocationTargetException;

/**
 * The default {@link CommandInstanceFactory}: creates the instances via their no-arg constructor. Your own factory can
 * delegate to it, e.g. for the classes that are not managed by the dependency injection container.
 *
 * @author fabian
 */
public class DefaultCommandInstanceFactory implements CommandInstanceFactory {

    /**
     * Create an instance of the given type via its no-arg constructor.
     *
     * @param type    the type to create an instance of
     * @param context the running test (not used)
     * @param <T>     generic type
     * @return the new instance
     * @throws Exception the exception of the constructor, or if there is no accessible no-arg constructor
     */
    @Override
    public <T> T create(final Class<T> type, final Context context) throws Exception {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (final InvocationTargetException e) {
            // report the exception of the constructor itself
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
