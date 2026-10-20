package com.jexunit.core.spi;

import com.jexunit.core.spi.data.DataProvider;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServiceRegistry (Singleton) for loading and registering extensions like data providers.
 *
 * @author fabian
 */
public class ServiceRegistry {

    private static final Logger LOG = Logger.getLogger(ServiceRegistry.class.getName());

    private static final Map<Class<?>, List<?>> services = new HashMap<>();

    private static volatile ServiceRegistry instance;

    private ServiceRegistry() {
    }

    /**
     * Initialize the ServiceRegistry singleton.
     */
    public static synchronized void initialize() {
        if (instance == null) {
            // publish the instance only after loading the extensions, as getInstance() doesn't synchronize
            final ServiceRegistry registry = new ServiceRegistry();
            registry.loadExtensions(DataProvider.class);
            instance = registry;
        }
    }

    /**
     * Load the extensions by type.
     *
     * @param type the type to lookup service implementations for
     */
    private <T> void loadExtensions(final Class<T> type) {
        final ServiceLoader<T> loader = ServiceLoader.load(type);
        loader.forEach(e -> register(type, e));
    }

    /**
     * Get the (singleton) instance of the ServiceRegistry.
     *
     * @return the (singleton) instance of the ServiceRegistry
     */
    public static ServiceRegistry getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    /**
     * Get all registered service implementations of the given type (i.e. DataProvider).
     *
     * @param type the type to get the registered implementations for
     * @return all registered service implementations of the given type
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getServicesFor(final Class<T> type) {
        return (List<T>) services.get(type);
    }

    /**
     * Create the DataProvider for the given test class. The registered DataProviders are asked in the order they were
     * found, and the last one that can provide the test data for the test class is used. A new instance of it is
     * created, as the DataProvider holds the state of the test class it is initialized for.
     *
     * @param testClass the test class to create the DataProvider for
     * @return a new (not yet initialized) instance of the DataProvider for the test class (or the registered instance
     * itself, if it has no accessible no-arg constructor), or <code>null</code> if no DataProvider can provide the test
     * data
     */
    public DataProvider createDataProvider(final Class<?> testClass) {
        DataProvider dataProvider = null;
        final List<DataProvider> dataProviders = getServicesFor(DataProvider.class);
        if (dataProviders != null) {
            for (final DataProvider dp : dataProviders) {
                if (dp.canProvide(testClass)) {
                    dataProvider = dp;
                }
            }
        }
        if (dataProvider == null) {
            return null;
        }
        try {
            return dataProvider.getClass().getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            LOG.log(Level.FINE, "DataProvider cannot be instantiated, using the registered instance", e);
            return dataProvider;
        }
    }

    /**
     * Register the service for the given type. This will check, if the service is an instance of the given type!
     *
     * @param type    the type to register the service for
     * @param service the service to register
     */
    @SuppressWarnings("unchecked")
    public <T> void register(final Class<T> type, final T service) {
        if (service == null || !type.isAssignableFrom(service.getClass())) {
            throw new IllegalArgumentException("The service to register should be a subtype of the given type!");
        }
        if (!services.containsKey(type)) {
            services.put(type, new ArrayList<T>());
        }
        ((List<T>) services.get(type)).add(service);
    }

}
