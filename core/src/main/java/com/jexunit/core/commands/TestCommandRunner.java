package com.jexunit.core.commands;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.Command.Type;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.model.TestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for running the test-commands.
 *
 * @author fabian
 */
public class TestCommandRunner {

    private final JExUnitBase testBase;

    public TestCommandRunner(final JExUnitBase testBase) {
        this.testBase = testBase;
    }

    /**
     * Get the method for the current testCommand (via the {@code @TestCommand}-Annotation) and call it.
     *
     * @param testCase the current testCase to run
     * @throws Exception in case that something goes wrong
     */
    @SuppressWarnings("unchecked")
    public void runTestCommand(final TestCase<?> testCase) throws Exception {
        // remove the parameters used by the framework
        removeFrameworkParameters(testCase);

        // check, which method to run for the current TestCommand
        final Command testCommand = TestCommandScanner.getTestCommand(testCase.getTestCommand().toLowerCase(),
                testBase.getTestType());
        if (testCommand != null) {
            // set the default value for fastFail if not set in the testCase
            if (testCase.getFastFail() == null) {
                testCase.setFastFail(testCommand.isFastFail());
            }

            // prepare the test-method
            final Method testMethod;
            if (testCommand.getType() == Type.METHOD) {
                testMethod = testCommand.getMethod();
            } else if (testCommand.getType() == Type.CLASS) {
                // prepare and run test-command defined by a class
                final Object testCommandInstance = testCommand.getImplementation().newInstance();
                TestContextManager.add((Class<Object>) testCommand.getImplementation(), testCommandInstance);

                // inject Test-Parameters (and -Context) to the class
                injectTestParams(testCase, testCommandInstance);

                // invoke the test-command
                testMethod = getSinglePublicMethod(testCommand);
            } else {
                throw new IllegalArgumentException("Type of the TestCommand has to be one of 'METHOD' or 'CLASS'!");
            }

            // prepare the parameters
            final List<Object> parameters = prepareParameters(testCase, testMethod);

            // invoke the method with the parameters
            invokeTestCommandMethod(testCommand, testMethod, parameters.toArray());
        } else {
            testBase.runCommand(testCase);
        }
    }

    /**
     * Check the given test command for a single public method and return this method. If there is no public method
     * declared, null will be returned. If there are multiple public methods found, an IllegalArgumentException will be
     * thrown because test commands of type class are allowed only a single public method!
     *
     * @param testCommand test command
     * @return method "behind" the test command
     */
    private Method getSinglePublicMethod(final Command testCommand) {
        Method method = null;
        for (final Method m : testCommand.getImplementation().getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                if (method == null) {
                    method = m;
                } else {
                    throw new IllegalArgumentException(
                            "Multiple public methods found in test command of type 'CLASS'. This is not allowed!");
                }
            }
        }
        return method;
    }

    /**
     * Remove the parameters used by the framework to only pass the "users" parameters to the commands.
     *
     * @param testCase current TestCase
     */
    private void removeFrameworkParameters(final TestCase<?> testCase) {
        for (final DefaultCommands dc : DefaultCommands.values()) {
            testCase.getValues().remove(JExUnitConfig.getDefaultCommandProperty(dc));
        }
    }

    /**
     * Prepare the parameters for the given method (representing the test-command implementation) out of the given
     * test-case.
     *
     * @param testCase the test-case to prepare the parameters from
     * @param method   the method representing the test-command implementation
     * @return the parameters-list to invoke the method with
     * @throws Exception in case that something goes wrong
     */
    private List<Object> prepareParameters(final TestCase<?> testCase, final Method method) throws Exception {
        final List<Object> parameters = new ArrayList<>(method.getParameterTypes().length);
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int i = 0;
        for (final Parameter parameter : method.getParameters()) {
            final Class<?> parameterType = parameter.getType();
            if (parameterType == TestCase.class) {
                parameters.add(testCase);
            } else if (parameterType == TestContext.class) {
                parameters.add(TestContextManager.getTestContext());
            } else {
                if (parameterAnnotations[i].length > 0) {
                    for (final Annotation a : parameterAnnotations[i]) {
                        if (a instanceof Context) {
                            // add an instance out of the test-context
                            final Context ctx = (Context) a;
                            final String id = ctx.value();
                            if (id.isEmpty()) {
                                // lookup the instance out of the current TestContext
                                parameters.add(TestContextManager.get(parameterType));
                            } else {
                                parameters.add(TestContextManager.get(parameterType, id));
                            }
                            break;
                        } else if (a instanceof TestParam) {
                            // add "single" test-param here
                            final TestParam param = (TestParam) a;
                            String key = param.value();
                            // read out the parameters name if key is NOT set and parameter name is
                            // present (possible since jdk 1.8 if compiler argument '-parameters' is
                            // set!
                            if (key.isEmpty() && parameter.isNamePresent()) {
                                // try to get the parameters name as key
                                key = parameter.getName();
                            }
                            final String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
                            final Object value = TestObjectHelper.convertPropertyStringToObject(parameterType, stringValue);
                            if (param.required() && value == null) {
                                throw new IllegalArgumentException("Required parameter not found: " + key);
                            }
                            parameters.add(value);
                        }
                    }
                } else {
                    final Object o = TestObjectHelper.createObject(testCase, parameterType);
                    parameters.add(o);
                }
            }
            i++;
        }
        return parameters;
    }

    /**
     * Prepare the attributes for the given test-command class. This will "inject" the attributes annotated with
     * <code>@TestParam</code>.
     *
     * @param testCase the test-case to prepare the parameters from
     * @param instance the instance representing the test-command implementation
     * @throws Exception in case that something goes wrong
     */
    private void injectTestParams(final TestCase<?> testCase, final Object instance) throws Exception {
        for (final Field field : instance.getClass().getDeclaredFields()) {
            final Annotation[] attributeAnnotations = field.getAnnotationsByType(TestParam.class);
            for (final Annotation a : attributeAnnotations) {
                if (a instanceof Context) {
                    // add an instance out of the test-context
                    final Context ctx = (Context) a;
                    final String id = ctx.value();
                    final Object value;
                    if (id.isEmpty()) {
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
                    final TestParam param = (TestParam) a;
                    String key = param.value();
                    // if key is not set, the field name will be the key
                    if (key.isEmpty()) {
                        // get the field name as key
                        key = field.getName();
                    }
                    final String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
                    final Object value = TestObjectHelper.convertPropertyStringToObject(field.getType(), stringValue);
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
     * Invoke the given method (representing the implementation of the test-command) with the given parameters. This
     * will invoke the method static, on the current test-class or on the instance out of the test-context. If there is
     * no instance in the test-context, a new instance will be created an put to the test-context.
     *
     * @param method     the method to invoke
     * @param parameters the parameters for the method
     * @throws Exception in case that something goes wrong
     */
    private void invokeTestCommandMethod(final Command testCommand, final Method method, final Object[] parameters) throws Exception {
        final Object o;
        if (method.getDeclaringClass() == testBase.getClass()) {
            o = this;
        } else if (Modifier.isStatic(method.getModifiers())) {
            o = null;
        } else {
            // create new instance of the Command-Class and put it to the test-context
            @SuppressWarnings("unchecked") final Class<Object> clazz = (Class<Object>) method.getDeclaringClass();
            Object instance = TestContextManager.get(clazz);
            if (instance == null) {
                instance = clazz.newInstance();
                TestContextManager.add(clazz, instance);
            }
            o = instance;
        }

        // invoke via TestCommandInvocationHandler to be able to proxy the call
        final Invocable invocationHandler = TestCommandInvocationHandler.getInvocationHandler(testCommand, method, o);
        invocationHandler.invoke(parameters);
    }

}