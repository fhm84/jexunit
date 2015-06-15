package com.jexunit.core.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.jexunit.core.spi.data.DataProvider;

public class ServiceRegistry {

	private static Map<Class<?>, List<?>> services = new HashMap<>();

	private static ServiceRegistry instace;

	private ServiceRegistry() {
	}

	public static void initialize() {
		if (instace == null) {
			instace = new ServiceRegistry();
			instace.loadExtensions(DataProvider.class);
		}
	}

	private <T> void loadExtensions(Class<T> type) {
		ServiceLoader<T> loader = ServiceLoader.load(type);
		loader.forEach(e -> register(type, e));
	}

	public static ServiceRegistry getInstance() {
		if (instace == null) {
			initialize();
		}
		return instace;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getServicesFor(Class<T> type) {
		return (List<T>) services.get(type);
	}

	@SuppressWarnings("unchecked")
	public <T> void register(Class<T> type, T service) {
		if (!services.containsKey(type)) {
			services.put(type, new ArrayList<T>());
		}
		((List<T>) services.get(type)).add(service);
	}

}
