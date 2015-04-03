package com.jexunit.core.context;

/**
 * The TestContextManager will "manage" the contexts for the tests. The TestContext will be put into a ThreadLocal
 * variable to be available in all commands for the current thread.<br>
 * The TestContext itself is "a simple" map. You can put a value identified by its type or by an id (string) into the
 * TestContext and get it out the same way.<br>
 * <u>Attention:</u> if you put a value into the context by its type, you can only put ONE instance because it will by
 * mapped by its classname!
 * 
 * @author fabian
 * 
 *         TODO: "inject" the TestContext into a "Command-Provider"/TestClass, ...
 * 
 */
public class TestContextManager {

	private static ThreadLocal<TestContext> context = new InheritableThreadLocal<TestContext>() {

		@Override
		protected TestContext initialValue() {
			return new TestContext();
		}
	};

	/**
	 * Get the TestContext.
	 * 
	 * @return the TestContext
	 */
	public static TestContext getTestContext() {
		return context.get();
	}

	/**
	 * Add an instance of type T to the context. An already existing instance of this type will be overridden!
	 * 
	 * @param type
	 *            the type of the instance to add
	 * @param instance
	 *            the instance to add to the context
	 * @param <T>
	 *            generic type
	 * @return the TestContext
	 */
	public static <T> TestContext add(Class<T> type, T instance) {
		return getTestContext().add(type, instance);
	}

	/**
	 * Add a value identified by the given id to the context. An already existing instance for this id will be
	 * overridden!
	 * 
	 * @param id
	 *            the id for the value (to get it out of the TestContext again)
	 * @param value
	 *            the value to add to the context
	 * @return the TestContext
	 */
	public static TestContext add(String id, Object value) {
		return getTestContext().add(id, value);
	}

	/**
	 * Get the instance of the given type out of the context.
	 * 
	 * @param type
	 *            the type of the instance to get
	 * @param <T>
	 *            generic type
	 * @return the instance identified by the given type if added into the context before, else null
	 */
	public static <T> T get(Class<T> type) {
		return getTestContext().get(type);
	}

	/**
	 * Get the value identified by the given id out of the context.
	 * 
	 * @param type
	 *            the type to cast the value to
	 * @param id
	 *            the id the value is identified by
	 * @param <T>
	 *            generic type
	 * @return the instance identified by the given id casted into the given type if added into the context before, else
	 *         null
	 */
	public static <T> T get(Class<T> type, String id) {
		return getTestContext().get(type, id);
	}
}
