package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.spi.CommandInstanceFactory;
import com.jexunit.core.spi.DefaultCommandInstanceFactory;

import java.util.Optional;

/**
 * Creates the instances of the command classes and command providers with the configured
 * {@link CommandInstanceFactory} ({@link JExUnitConfig.ConfigKey#COMMAND_INSTANCE_FACTORY}).
 *
 * @author fabian
 */
final class CommandInstances {

    private static final CommandInstanceFactory DEFAULT_FACTORY = new DefaultCommandInstanceFactory();

    /**
     * The configured factory, created once per class name (the configuration may change between tests).
     */
    private static String factoryClassName;
    private static CommandInstanceFactory factory;

    private CommandInstances() {
    }

    /**
     * Create an instance of the given type with the configured factory.
     *
     * @param type the command class or command provider
     * @param host the host of the running test
     * @param <T>  generic type
     * @return the new instance
     * @throws Exception if the instance cannot be created
     */
    static <T> T create(final Class<T> type, final TestCommandHost host) throws Exception {
        final CommandInstanceFactory instanceFactory = getFactory();
        final T instance;
        try {
            instance = instanceFactory.create(type, new HostContext(host));
        } catch (final VirtualMachineError e) {
            throw e;
        } catch (final Error e) {
            // e.g. an AssertionError of a constructor: the command didn't run, so this must not satisfy an expected
            // exception (the runner reports an exception while preparing the command as setup error)
            throw new IllegalStateException(String.format("%s could not be created: %s", type.getName(), e), e);
        }
        if (instance == null) {
            throw new IllegalStateException(String.format("The CommandInstanceFactory %s returned null for %s",
                    instanceFactory.getClass().getName(), type.getName()));
        }
        if (!type.isInstance(instance)) {
            throw new IllegalStateException(String.format(
                    "The CommandInstanceFactory %s returned an instance of %s for %s",
                    instanceFactory.getClass().getName(), instance.getClass().getName(), type.getName()));
        }
        return instance;
    }

    /**
     * Get the configured factory, or the default factory if none is configured.
     *
     * @return the factory
     * @throws ReflectiveOperationException if the configured factory cannot be created
     */
    static synchronized CommandInstanceFactory getFactory() throws ReflectiveOperationException {
        final String className = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_INSTANCE_FACTORY);
        if (className == null || className.trim().isEmpty()) {
            return DEFAULT_FACTORY;
        }
        if (!className.trim().equals(factoryClassName)) {
            final Class<?> factoryClass = Class.forName(className.trim());
            if (!CommandInstanceFactory.class.isAssignableFrom(factoryClass)) {
                throw new IllegalArgumentException(String.format("%s (configured in %s) is no %s", className,
                        JExUnitConfig.ConfigKey.COMMAND_INSTANCE_FACTORY.getKey(),
                        CommandInstanceFactory.class.getName()));
            }
            factory = (CommandInstanceFactory) factoryClass.getDeclaredConstructor().newInstance();
            factoryClassName = className.trim();
        }
        return factory;
    }

    /**
     * The running test, provided by the host.
     */
    private static final class HostContext implements CommandInstanceFactory.Context {

        private final TestCommandHost host;

        HostContext(final TestCommandHost host) {
            this.host = host;
        }

        @Override
        public Object getTestInstance() {
            return host.getTestInstance();
        }

        @Override
        public <T> Optional<T> unwrap(final Class<T> type) {
            final Object frameworkContext = host.getFrameworkContext();
            return type.isInstance(frameworkContext) ? Optional.of(type.cast(frameworkContext)) : Optional.empty();
        }
    }

}
