package com.jexunit.core.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.Command.Type;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.model.TestCase;

/**
 * Helper class for running the test-commands.
 * 
 * @author fabian
 * 
 */
public class TestCommandRunner {

	JExUnitBase testBase;

	public TestCommandRunner(JExUnitBase testBase) {
		this.testBase = testBase;
	}

	/**
	 * Get the method for the current testCommand (via the {@code @TestCommand}-Annotation) and call
	 * it.
	 * 
	 * @param testCase
	 *            the current testCase to run
	 * 
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	@SuppressWarnings("unchecked")
	public void runTestCommand(TestCase testCase) throws Exception {
		// remove the parameters used by the framework
		removeFrameworkParameters(testCase);

		// check, which method to run for the current TestCommand
		// Method method = TestCommandScanner.getTestCommandMethod(testCase.getTestCommand()
		// .toLowerCase(), testBase.getTestType());
		Command testCommand = TestCommandScanner.getTestCommand(testCase.getTestCommand()
				.toLowerCase(), testBase.getTestType());
		if (testCommand != null) {
			if (testCommand.getType() == Type.METHOD) {
				// prepare the parameters
				List<Object> parameters = prepareParameters(testCase, testCommand.getMethod());

				// invoke the method with the parameters
				invokeTestCommandMethod(testCommand.getMethod(), parameters.toArray());
			} else if (testCommand.getType() == Type.CLASS) {
				// prepare and run test-command defined by a class
				Object testCommandInstance = testCommand.getImplementation().newInstance();
				TestContextManager.add((Class<Object>) testCommand.getImplementation(),
						testCommandInstance);

				// inject Test-Parameters (and -Context) to the class
				injectTestParams(testCase, testCommandInstance);

				// invoke the test-command
				Method m = testCommand.getImplementation().getDeclaredMethods()[0];
				// prepare the parameters
				List<Object> parameters = prepareParameters(testCase, m);

				// invoke the method with the parameters
				invokeTestCommandMethod(m, parameters.toArray());
			}
		} else {
			testBase.runCommand(testCase);
		}
	}

	/**
	 * Remove the parameters used by the framework to only pass the "users" parameters to the
	 * commands.
	 * 
	 * @param testCase
	 *            current TestCase
	 */
	private void removeFrameworkParameters(TestCase testCase) {
		for (DefaultCommands dc : DefaultCommands.values()) {
			testCase.getValues().remove(JExUnitConfig.getStringProperty(dc.getConfigKey()));
		}
	}

	/**
	 * Prepare the parameters for the given method (representing the test-command implementation)
	 * out of the given test-case.
	 * 
	 * @param testCase
	 *            the test-case to prepare the parameters from
	 * @param method
	 *            the method representing the test-command implementation
	 * @return the parameters-list to invoke the method with
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	private List<Object> prepareParameters(TestCase testCase, Method method) throws Exception {
		List<Object> parameters = new ArrayList<>(method.getParameterTypes().length);
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int i = 0;
		for (Parameter parameter : method.getParameters()) {
			Class<?> parameterType = parameter.getType();
			if (parameterType == TestCase.class) {
				parameters.add(testCase);
			} else if (parameterType == TestContext.class) {
				parameters.add(TestContextManager.getTestContext());
			} else {
				if (parameterAnnotations[i].length > 0) {
					for (Annotation a : parameterAnnotations[i]) {
						if (a instanceof Context) {
							// add an instance out of the test-context
							Context ctx = (Context) a;
							String id = ctx.value();
							if (id == null || id.isEmpty()) {
								// lookup the instance out of the current TestContext
								parameters.add(TestContextManager.get(parameterType));
							} else {
								parameters.add(TestContextManager.get(parameterType, id));
							}
							break;
						} else if (a instanceof TestParam) {
							// add "single" test-param here
							TestParam param = (TestParam) a;
							String key = param.value();
							// read out the parameters name if key is NOT set and parameter name is
							// present (possible since jdk 1.8 if compiler argument '-parameters' is
							// set!
							if ((key == null || key.isEmpty()) && parameter.isNamePresent()) {
								// try to get the parameters name as key
								key = parameter.getName();
							}
							String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
							Object value = TestObjectHelper.convertPropertyStringToObject(
									parameterType, stringValue);
							if (param.required() && value == null) {
								throw new IllegalArgumentException("Required parameter not found: "
										+ key);
							}
							parameters.add(value);
						}
					}
				} else {
					Object o = TestObjectHelper.createObject(testCase, parameterType);
					parameters.add(o);
				}
			}
			i++;
		}
		return parameters;
	}

	/**
	 * Prepare the attributes for the given test-command class. This will "inject" the attributes
	 * annotated with @TestParam.
	 * 
	 * @param testCase
	 *            the test-case to prepare the parameters from
	 * @param instance
	 *            the instance representing the test-command implementation
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	private void injectTestParams(TestCase testCase, Object instance) throws Exception {
		for (Field field : instance.getClass().getDeclaredFields()) {
			Annotation[] attributeAnnotations = field.getAnnotationsByType(TestParam.class);
			for (Annotation a : attributeAnnotations) {
				if (a instanceof Context) {
					// add an instance out of the test-context
					Context ctx = (Context) a;
					String id = ctx.value();
					Object value;
					if (id == null || id.isEmpty()) {
						// lookup the instance out of the current TestContext
						value = TestContextManager.get(field.getType());
					} else {
						value = TestContextManager.get(field.getType(), id);
					}
					if (field.isAccessible()) {
						field.set(instance, value);
					} else {
						field.setAccessible(true);
						field.set(instance, value);
						field.setAccessible(false);
					}
					break;
				} else if (a instanceof TestParam) {
					// add "single" test-param here
					TestParam param = (TestParam) a;
					String key = param.value();
					// if key is not set, the field name will be the key
					if (key == null || key.isEmpty()) {
						// get the field name as key
						key = field.getName();
					}
					String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
					Object value = TestObjectHelper.convertPropertyStringToObject(field.getType(),
							stringValue);
					if (param.required() && value == null) {
						throw new IllegalArgumentException("Required parameter not found: " + key);
					}
					if (field.isAccessible()) {
						field.set(instance, value);
					} else {
						field.setAccessible(true);
						field.set(instance, value);
						field.setAccessible(false);
					}
				}
			}
		}
	}

	/**
	 * Invoke the given method (representing the implementation of the test-command) with the given
	 * parameters. This will invoke the method static, on the current test-class or on the instance
	 * out of the test-context. If there is no instance in the test-context, a new instance will be
	 * created an put to the test-context.
	 * 
	 * @param method
	 *            the method to invoke
	 * @param parameters
	 *            the parameters for the method
	 * @throws Exception
	 *             in case that something goes wrong
	 */
	private void invokeTestCommandMethod(Method method, Object[] parameters) throws Exception {
		try {
			if (method.getDeclaringClass() == testBase.getClass()) {
				method.invoke(this, parameters);
			} else if (Modifier.isStatic(method.getModifiers())) {
				// invoke static
				method.invoke(null, parameters);
			} else {
				// create new instance of the Command-Class and put it to the test-context
				@SuppressWarnings("unchecked")
				Class<Object> clazz = (Class<Object>) method.getDeclaringClass();
				Object instance = TestContextManager.get(clazz);
				if (instance == null) {
					instance = clazz.newInstance();
					TestContextManager.add(clazz, instance);
				}
				method.invoke(instance, parameters);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			e.printStackTrace();
			throw e;
		} catch (InvocationTargetException e) {
			Throwable t = e;
			while (t.getCause() != null) {
				t = t.getCause();
			}
			if (t instanceof AssertionError) {
				throw (AssertionError) t;
			}
			throw e;
		}
	}

}