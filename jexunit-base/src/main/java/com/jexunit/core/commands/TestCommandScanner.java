package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.Command.Type;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestCommand.TestCommands;
import com.jexunit.core.dataprovider.ExcelFile;
import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.MethodReporter;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MethodReporter-Implementation for "storing" the annotated methods found. The "Annotation-Scan" will run once, so we
 * can hold the methods found in the static methods-Map.
 * <p>
 * A command can have several implementations for the same type (e.g. overloaded methods). The implementation to run is
 * chosen by the {@link TestCommandRunner} for each test case.
 *
 * @author fabian
 */
public class TestCommandScanner implements TypeReporter, MethodReporter {

    private static final Logger LOG = Logger.getLogger(TestCommandScanner.class.getName());

    private static final Map<String, Map<Class<?>, List<Command>>> commands = new HashMap<>();

    /**
     * Classes whose command methods are registered already. The scan reports each annotated method, but all the
     * command methods of a class are registered at once.
     */
    private static final Set<String> scannedMethodClasses = new HashSet<>();

    private static volatile boolean initialized = false;

    public static void ensureScanned() {
        if (initialized) return;
        synchronized (TestCommandScanner.class) {
            if (initialized) return;
            final AnnotationDetector detector = new AnnotationDetector(new TestCommandScanner());
            try {
                JExUnitConfig.init();
                final String property = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.ANNOTATION_SCAN_PACKAGE);
                if (property != null && !property.isEmpty()) {
                    detector.detect(property.split(","));
                } else {
                    detector.detect();
                }
            } catch (final IOException e) {
                // without the scan there are no test commands, so don't continue silently
                throw new IllegalStateException("Scanning the classpath for test commands failed", e);
            }
            initialized = true;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Annotation>[] annotations() {
        return new Class[]{TestCommand.class, TestCommands.class};
    }

    @Override
    public void reportMethodAnnotation(final Class<? extends Annotation> annotation, final String className, final String methodName) {
        if (!scannedMethodClasses.add(className)) {
            return;
        }
        try {
            final Class<?> clazz = loadClass(className);
            Class<?> type = null;
            if (hasExcelFileAnnotation(clazz)) {
                type = clazz;
            }

            if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
                for (final Method m : clazz.getDeclaredMethods()) {
                    final TestCommand[] testCommands = m.getDeclaredAnnotationsByType(TestCommand.class);
                    registerCommands(testCommands, type, m);
                }
            }
        } catch (final ClassNotFoundException | LinkageError e) {
            LOG.log(Level.WARNING, "Test commands of class " + className + " are skipped, the class cannot be loaded", e);
        }
    }

    @Override
    public void reportTypeAnnotation(final Class<? extends Annotation> annotation, final String className) {
        try {
            final Class<?> clazz = loadClass(className);

            if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
                if (!checkTestCommandClass(clazz)) {
                    throw new IllegalArgumentException(
                            "Test-Command implementation not valid. A class defined as test-command has to provide one single public method!");
                }

                final TestCommand[] testCommands = clazz.getDeclaredAnnotationsByType(TestCommand.class);
                registerCommands(testCommands, clazz, null);
            }
        } catch (final ClassNotFoundException | LinkageError e) {
            LOG.log(Level.WARNING, "Test commands of class " + className + " are skipped, the class cannot be loaded", e);
        }
    }

    /**
     * Load the class reported by the annotation scan. The scan finds the classes via the context class loader (if set),
     * so they are loaded the same way.
     *
     * @param className the name of the class to load
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    private static Class<?> loadClass(final String className) throws ClassNotFoundException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader classLoader =
                contextClassLoader != null ? contextClassLoader : TestCommandScanner.class.getClassLoader();
        return classLoader.loadClass(className);
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
                    command = command.toLowerCase(Locale.ROOT);
                    final Command implementation;
                    final Class<?> key;
                    if (method == null) {
                        // test-command is implemented in a class
                        implementation = new Command(command, type, tc.fastFail());
                        key = null;
                    } else {
                        // test-command is a method
                        implementation = new Command(command, type, method, tc.fastFail());
                        key = type;
                    }
                    // further implementations for the same command and type (e.g. overloaded methods) are added, the
                    // runner chooses one of them for each test case
                    final List<Command> implementations = commands.computeIfAbsent(command, c -> new HashMap<>())
                            .computeIfAbsent(key, k -> new ArrayList<>());
                    if (!implementations.contains(implementation)) {
                        implementations.add(implementation);
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
        if (prefix != null && name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            name = name.substring(prefix.length());
        }
        if (postfix != null && name.toLowerCase(Locale.ROOT).endsWith(postfix.toLowerCase(Locale.ROOT))) {
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
        if (prefix != null && name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            name = name.substring(prefix.length());
        }
        if (postfix != null && name.toLowerCase(Locale.ROOT).endsWith(postfix.toLowerCase(Locale.ROOT))) {
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
     * Check if the given class has any field or method annotated with {@code @ExcelFile}. This is used to identify
     * test classes that use JExUnit's Excel-based data loading.
     *
     * @param clazz Class to check
     * @return true if the class has a field or method annotated with {@code @ExcelFile}, else false
     */
    private boolean hasExcelFileAnnotation(final Class<?> clazz) {
        for (final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcelFile.class)) {
                return true;
            }
        }
        for (final java.lang.reflect.Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(ExcelFile.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the implementations of the Command for the given command-name and type. The implementations of the type
     * (or its superclasses) take precedence over the ones defined outside of test classes.
     *
     * @param command the excel-command
     * @param clazz   the type of the test-class
     * @return the implementations of the command for the given class (several e.g. for overloaded methods), or an
     * empty list if not found
     */
    static List<Command> getTestCommands(final String command, final Class<?> clazz) {
        final Map<Class<?>, List<Command>> cmds = commands.get(command);
        if (cmds == null || cmds.isEmpty()) {
            return Collections.emptyList();
        }
        if (clazz != null) {
            // not found? check superclass
            Class<?> cls = clazz;
            do {
                if (cmds.containsKey(cls)) {
                    final List<Command> found = new ArrayList<>();
                    for (final Command c : cmds.get(cls)) {
                        if (c.getType() == Type.CLASS || c.getImplementation() == cls) {
                            found.add(c);
                        }
                    }
                    if (!found.isEmpty()) {
                        return found;
                    }
                }
            } while ((cls = cls.getSuperclass()) != null);
        }
        return cmds.getOrDefault(null, Collections.emptyList());
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

        // the built-in commands (respecting the configured names and prefix)
        return JExUnitConfig.getDefaultCommandProperty(DefaultCommands.DISABLED).equalsIgnoreCase(s)
                || JExUnitConfig.getDefaultCommandProperty(DefaultCommands.REPORT).equalsIgnoreCase(s);
    }

}
