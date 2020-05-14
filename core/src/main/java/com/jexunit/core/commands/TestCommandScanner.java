package com.jexunit.core.commands;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.Command.Type;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestCommand.TestCommands;
import eu.infomas.annotation.AnnotationDetector.MethodReporter;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;
import org.junit.runner.RunWith;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * MethodReporter-Implementation for "storing" the annotated methods found. The "Annotation-Scan" will run once, so we
 * can hold the methods found in the static methods-Map.
 * <p>
 * TODO: "override" command-methods (what about different parameter-types?)
 *
 * @author fabian
 */
public class TestCommandScanner implements TypeReporter, MethodReporter {

    private static final Map<String, Map<Class<?>, Command>> commands = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Annotation>[] annotations() {
        return new Class[]{TestCommand.class, TestCommands.class};
    }

    @Override
    public void reportMethodAnnotation(final Class<? extends Annotation> annotation, final String className, final String methodName) {
        try {
            final Class<?> clazz = getClass().getClassLoader().loadClass(className);
            Class<?> type = null;
            if (clazz.isAnnotationPresent(RunWith.class)) {
                final RunWith rwa = clazz.getAnnotation(RunWith.class);
                if (rwa.value() == JExUnit.class) {
                    type = clazz;
                }
            }

            if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
                for (final Method m : clazz.getDeclaredMethods()) {
                    final TestCommand[] testCommands = m.getDeclaredAnnotationsByType(TestCommand.class);
                    registerCommands(testCommands, type, m);
                }
            }
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reportTypeAnnotation(final Class<? extends Annotation> annotation, final String className) {
        try {
            final Class<?> clazz = getClass().getClassLoader().loadClass(className);

            if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
                if (!checkTestCommandClass(clazz)) {
                    throw new IllegalArgumentException(
                            "Test-Command implementation not valid. A class defined as test-command has to provide one single public method!");
                }

                final TestCommand[] testCommands = clazz.getDeclaredAnnotationsByType(TestCommand.class);
                registerCommands(testCommands, clazz, null);
            }
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate the command names for the given Annotations and "register" the test-commands.
     *
     * @param testCommands TestCommand-Annotations found
     * @param type         the type the annotations were found in
     * @param method       the method the annotations were found for
     */
    private void registerCommands(final TestCommand[] testCommands, final Class<?> type, final Method method) {
        for (final TestCommand tc : testCommands) {
            if (tc != null) {
                String[] commandNames = tc.value();
                if (commandNames.length == 0) {
                    // calculate command-name out of the method-name/class-name
                    if (method == null) {
                        commandNames = new String[]{calculateCommandName(type)};
                    } else {
                        commandNames = new String[]{calculateCommandName(method)};
                    }
                }
                for (String command : commandNames) {
                    command = command.toLowerCase();
                    if (!commands.containsKey(command)) {
                        commands.put(command, new HashMap<>());
                    }
                    if (method == null) {
                        // test-command is implemented in a class
                        commands.get(command).put(null, new Command(command, type, tc.fastFail()));
                    } else {
                        // test-command is a method
                        commands.get(command).put(type, new Command(command, type, method, tc.fastFail()));
                    }
                }
            }
        }
    }

    /**
     * Calculate the name for the test-command out of the simple name of the given class. If there is a prefix and/or
     * postfix configured, this will be removed from the name.
     *
     * @param type Class to calculate the name for the test-command from
     * @return the calculated name for the test-command
     */
    private String calculateCommandName(final Class<?> type) {
        String name = type.getSimpleName();
        final String prefix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_CLASS_PREFIX);
        final String postfix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_CLASS_POSTFIX);
        if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
            name = name.substring(prefix.length());
        }
        if (name.toLowerCase().endsWith(postfix.toLowerCase())) {
            name = name.substring(0, name.length() - postfix.length());
        }

        return name;
    }

    /**
     * Calculate the name for the test-command out of the name of the given method. If there is a prefix and/or postfix
     * configured, this will be removed from the name.
     *
     * @param m Method to calculate the name for the test-command from
     * @return the calculated name for the test-command
     */
    private String calculateCommandName(final Method m) {
        String name = m.getName();
        final String prefix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_METHOD_PREFIX);
        final String postfix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_METHOD_POSTFIX);
        if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
            name = name.substring(prefix.length());
        }
        if (name.toLowerCase().endsWith(postfix.toLowerCase())) {
            name = name.substring(0, name.length() - postfix.length());
        }

        return name;
    }

    /**
     * Check, if the given class is a valid test-command implementation. A class is a valid test-command implementation,
     * if there is only a single public method (functional interface!).
     *
     * @param clazz Class to check
     * @return true, if the class defines exactly one single public method, else false
     */
    private boolean checkTestCommandClass(final Class<?> clazz) {
        // check number of public methods
        final Method[] methods = clazz.getDeclaredMethods();
        int numberOfPublicMethods = 0;
        for (final Method m : methods) {
            if (Modifier.isPublic(m.getModifiers())) {
                numberOfPublicMethods++;
            }
        }

        return numberOfPublicMethods == 1;
    }

    /**
     * Get the Command for the given command-name and type.
     *
     * @param command the excel-command
     * @param clazz   the type of the test-class
     * @return the command for the given class, if found, else null
     */
    static Command getTestCommand(final String command, final Class<?> clazz) {
        if (commands.containsKey(command)) {
            final Map<Class<?>, Command> cmds = commands.get(command);

            if (cmds != null && !cmds.isEmpty()) {
                if (clazz == null) {
                    if (cmds.containsKey(null)) {
                        return cmds.get(null);
                    }
                } else {
                    // not found? check superclass
                    Class<?> cls = clazz;
                    do {
                        if (cmds.containsKey(cls)) {
                            final Command c = cmds.get(cls);
                            if (c.getType() == Type.CLASS || c.getImplementation() == cls) {
                                return c;
                            }
                        }
                    } while ((cls = cls.getSuperclass()) != Object.class);
                }
                return cmds.get(null);
            }
        }
        return null;
    }

    /**
     * Checks if the passed command exists.
     *
     * @param s command name to check
     * @return <code>true</code> if there is a command-implementation for the given string, else <code>false</code>
     */
    public static boolean isTestCommandValid(final String s) {
        if (commands.containsKey(s)) {
            return true;
        }

        boolean isDefaultCommand = false;
        for (final DefaultCommands defaultCommands : DefaultCommands.values()) {
            isDefaultCommand = defaultCommands.getDefaultValue().equals(s);
            if (isDefaultCommand) {
                break;
            }
        }
        return isDefaultCommand;
    }

}
