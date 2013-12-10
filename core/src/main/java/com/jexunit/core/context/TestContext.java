package com.jexunit.core.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The context for testing. This will be available in all test-commands, so you can put something
 * into or get something out of the context. You can put everything.<br>
 * <u>Attention:</u> if you put a value by its type, the classname of the type will be used as the
 * id. This way you can only put ONE instance per type! If you want to put multiple instances of the
 * same type, you have to think about the "id" yourself!
 * 
 * @author fabian
 * 
 */
public class TestContext {

	private ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

	public <T> TestContext add(Class<T> type, T value) {
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}
		return add(type.getName(), value);
	}

	public TestContext add(String id, Object value) {
		if (value == null) {
			throw new IllegalArgumentException("Value must be specified");
		}

		store.put(id, value);
		return this;
	}

	public <T> T get(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}

		return type.cast(store.get(type.getName()));
	}

	public <T> T get(Class<T> type, String id) {
		if (id == null) {
			throw new IllegalArgumentException("Id must be specified");
		}
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}

		return type.cast(store.get(id));
	}

	public TestContext clear() {
		store.clear();
		return this;
	}

}
