package com.jexunit.core.commands;

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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Helper class for running the test-commands.
 *
 * @author fabian
 */
public class TestCommandRunner {

    private final TestCommandHost testBase;

    public TestCommandRunner(final TestCommandHost testBase) {
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
        final List<Command> implementations = TestCommandScanner.getTestCommands(
                testCase.getTestCommand().toLowerCase(Locale.ROOT), testBase.getTestType());
        if (!implementations.isEmpty()) {
            final Command testCommand = selectImplementation(testCase, implementations);

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
                if (testMethod == null) {
                    throw new IllegalArgumentException(
                            "No public method found in test command class: " + testCommand.getImplementation().getName());
                }
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
     * Select the implementation to run if there are several for the command (e.g. overloaded methods). An
     * implementation can run if the test case has a value for each of its required {@code @TestParam}s. Of these, the
     * one with the most {@code @TestParam}s with a value is selected. Other parameters (the test case, the test
     * context, entities) are not considered.
     *
     * @param testCase        the current test case
     * @param implementations the implementations of the command
     * @return the implementation to run
     * @throws NoSuchMethodError if no implementation can run, or several match equally well. Like a missing
     *                            implementation, this is an error (not an exception), so it never satisfies an
     *                            expected exception
     */
    private Command selectImplementation(final TestCase<?> testCase, final List<Command> implementations) {
        if (implementations.size() == 1) {
            return implementations.get(0);
        }

        final List<Command> best = new ArrayList<>();
        int bestMatches = -1;
        for (final Command implementation : implementations) {
            final int matches = countTestParamValues(testCase, getImplementationMethod(implementation));
            if (matches < 0) {
                continue;
            }
            if (matches > bestMatches) {
                best.clear();
                bestMatches = matches;
            }
            if (matches == bestMatches) {
                best.add(implementation);
            }
        }

        if (best.isEmpty()) {
            throw new NoSuchMethodError(String.format(
                    "No implementation of test command '%s' has values for all its required parameters: %s",
                    testCase.getTestCommand(), describe(implementations)));
        }
        if (best.size() > 1) {
            throw new NoSuchMethodError(String.format(
                    "Test command '%s' is ambiguous, several implementations match the values equally well: %s",
                    testCase.getTestCommand(), describe(best)));
        }
        return best.get(0);
    }

    /**
     * Count the {@code @TestParam}s of the given method the test case has a (non-empty) value for.
     *
     * @param testCase the current test case
     * @param method   the method implementing the test command (null, if not found)
     * @return the number of {@code @TestParam}s with a value, or -1 if a required one has no value
     */
    private int countTestParamValues(final TestCase<?> testCase, final Method method) {
        if (method == null) {
            return 0;
        }
        int count = 0;
        for (final Parameter parameter : method.getParameters()) {
            final TestParam param = parameter.getAnnotation(TestParam.class);
            if (param == null || parameter.isAnnotationPresent(Context.class)) {
                continue;
            }
            final String value = TestObjectHelper.getPropertyByKey(testCase, getTestParamKey(parameter, param));
            if (value != null && !value.isEmpty()) {
                count++;
            } else if (param.required()) {
                return -1;
            }
        }
        return count;
    }

    private String describe(final List<Command> implementations) {
        return implementations.stream().map(implementation -> {
            final Method method = getImplementationMethod(implementation);
            if (method == null) {
                return implementation.getImplementation().getName();
            }
            return method.getDeclaringClass().getSimpleName() + "." + method.getName()
                    + Arrays.stream(method.getParameters()).map(parameter -> {
                final TestParam param = parameter.getAnnotation(TestParam.class);
                return (param != null ? "@TestParam(\"" + getTestParamKey(parameter, param) + "\") " : "")
                        + parameter.getType().getSimpleName();
            }).collect(Collectors.joining(", ", "(", ")"));
        }).collect(Collectors.joining(", "));
    }

    /**
     * Get the method implementing the given test command: the method itself, or the single public method of a test
     * command class.
     *
     * @param testCommand test command
     * @return method "behind" the test command, or null if a test command class has no public method
     */
    private Method getImplementationMethod(final Command testCommand) {
        return testCommand.getType() == Type.METHOD ? testCommand.getMethod() : getSinglePublicMethod(testCommand);
    }

    /**
     * Get the key of the given {@code @TestParam}: its value, or the name of the parameter if no value is set.
     *
     * @param parameter the parameter annotated with {@code @TestParam}
     * @param param     the annotation
     * @return the key to look up the value in the test case
     */
    private static String getTestParamKey(final Parameter parameter, final TestParam param) {
        String key = param.value();
        // read out the parameters name if key is NOT set and parameter name is present (possible since jdk 1.8 if
        // compiler argument '-parameters' is set!
        if (key.isEmpty() && parameter.isNamePresent()) {
            // try to get the parameters name as key
            key = parameter.getName();
        }
        return key;
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
                    boolean handled = false;
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
                            handled = true;
                            break;
                        } else if (a instanceof TestParam) {
                            // add "single" test-param here
                            final TestParam param = (TestParam) a;
                            final String key = getTestParamKey(parameter, param);
                            final String stringValue = TestObjectHelper.getPropertyByKey(testCase, key);
                            final Object value = TestObjectHelper.convertPropertyStringToObject(parameterType, stringValue);
                            if (param.required() && value == null) {
                                throw new IllegalArgumentException("Required parameter not found: " + key);
                            }
                            parameters.add(value);
                            handled = true;
                        }
                    }
                    if (!handled) {
                        parameters.add(TestObjectHelper.createObject(testCase, parameterType));
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
        final Object hostInstance = testBase.getTestClassInstance(method.getDeclaringClass());
        if (hostInstance != null) {
            o = hostInstance;
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
