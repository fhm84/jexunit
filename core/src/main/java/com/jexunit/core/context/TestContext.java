package com.jexunit.core.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The context for testing. This will be available in all test-commands, so you can put something into or get something
 * out of the context. You can put everything.<br>
 * <u>Attention:</u> if you put a value by its type, the classname of the type will be used as the id. This way you can
 * only put ONE instance per type! If you want to put multiple instances of the same type, you have to think about the
 * "id" yourself!
 * 
 * @author fabian
 * 
 */
public class TestContext {

	private ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

	/**
	 * Add the given value of type T to the TestContext. This will add the value by its type as key (for lookup).<br>
	 * Attention: you can only have one instance per key in the TestContext!
	 * 
	 * @param type
	 *            the type of the value (used also as key)
	 * @param value
	 *            the value to add to the TestContext
	 * @param <T>
	 *            generic type
	 * @return the TestContext itself (fluent API)
	 */
	public <T> TestContext add(Class<T> type, T value) {
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}
		return add(type.getName(), value);
	}

	/**
	 * Add the given value to the TestContext identified by the given id (to get it out again).
	 * 
	 * @param id
	 *            the id to identify the value inside the TestContext
	 * @param value
	 *            the value to add to the TestContext
	 * @return the TestContext itself (fluent API)
	 */
	public TestContext add(String id, Object value) {
		if (value == null) {
			throw new IllegalArgumentException("Value must be specified");
		}

		store.put(id, value);
		return this;
	}

	/**
	 * Get the value of the given type out of the TestContext. The type will be used as key for lookup and as cast-type
	 * for the value.
	 * 
	 * @param type
	 *            the type of the value to get from the TestContext (used as key)
	 * @param <T>
	 *            generic type
	 * @return the value out of the context if found, else null
	 */
	public <T> T get(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}

		return type.cast(store.get(type.getName()));
	}

	/**
	 * Get the value added before to the TestContext by the given id out of the TestContext. The type is used to cast
	 * the value.
	 * 
	 * @param type
	 *            the type to cast the value to
	 * @param id
	 *            the id of the value added to the TestContext
	 * @param <T>
	 *            generic type
	 * @return the value out of the TestContext if found identified by the given id, else null
	 */
	public <T> T get(Class<T> type, String id) {
		if (id == null) {
			throw new IllegalArgumentException("Id must be specified");
		}
		if (type == null) {
			throw new IllegalArgumentException("Type must be specified");
		}

		return type.cast(store.get(id));
	}

	/**
	 * Clear the TestContext. This will remove all entries of the TestContext.
	 * 
	 * @return the TestContext itself (fluent API)
	 */
	public TestContext clear() {
		store.clear();
		return this;
	}

}
