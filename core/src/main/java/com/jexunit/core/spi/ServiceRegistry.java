package com.jexunit.core.spi;

import com.jexunit.core.spi.data.DataProvider;

import java.util.*;

/**
 * ServiceRegistry (Singleton) for loading and registering extensions like data providers.
 *
 * @author fabian
 */
public class ServiceRegistry {

    private static final Map<Class<?>, List<?>> services = new HashMap<>();

    private static ServiceRegistry instace;

    private ServiceRegistry() {
    }

    /**
     * Initialize the ServiceRegistry singleton.
     */
    public static synchronized void initialize() {
        if (instace == null) {
            instace = new ServiceRegistry();
            instace.loadExtensions(DataProvider.class);
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
        if (instace == null) {
            initialize();
        }
        return instace;
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
