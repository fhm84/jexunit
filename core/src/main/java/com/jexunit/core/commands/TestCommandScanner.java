package com.jexunit.core.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.runner.RunWith;

import com.jexunit.core.JExUnit;
import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.Command.Type;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestCommand.TestCommands;

import eu.infomas.annotation.AnnotationDetector.MethodReporter;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;

/**
 * MethodReporter-Implementation for "storing" the annotated methods found. The "Annotation-Scan" will run once, so we
 * can hold the methods found in the static methods-Map.
 * 
 * TODO: "override" command-methods (what about different parameter-types?)
 * 
 * @author fabian
 * 
 */
public class TestCommandScanner implements TypeReporter, MethodReporter {

	private static final Map<String, Map<Class<?>, Command>> commands = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Annotation>[] annotations() {
		return new Class[] { TestCommand.class, TestCommands.class };
	}

	@Override
	public void reportMethodAnnotation(Class<? extends Annotation> annotation, String className, String methodName) {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(className);
			Class<?> type = null;
			if (clazz.isAnnotationPresent(RunWith.class)) {
				RunWith rwa = clazz.getAnnotation(RunWith.class);
				if (rwa.value() == JExUnit.class) {
					type = clazz;
				}
			}

			if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
				for (Method m : clazz.getDeclaredMethods()) {
					TestCommand[] testCommands = m.getDeclaredAnnotationsByType(TestCommand.class);
					registerCommands(testCommands, type, m);
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(className);

			if (annotation.isAnnotation() && (annotation == TestCommand.class || annotation == TestCommands.class)) {
				if (!checkTestCommandClass(clazz)) {
					throw new IllegalArgumentException(
							"Test-Command implementation not valid. A class defined as test-command has to provide one single public method!");
				}

				TestCommand[] testCommands = clazz.getDeclaredAnnotationsByType(TestCommand.class);
				registerCommands(testCommands, clazz, null);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculate the command names for the given Annotations and "register" the test-commands.
	 * 
	 * @param testCommands
	 *            TestCommand-Annotations found
	 * @param type
	 *            the type the annotations were found in
	 * @param method
	 *            the method the annotations were found for
	 */
	private void registerCommands(TestCommand[] testCommands, Class<?> type, Method method) {
		for (TestCommand tc : testCommands) {
			if (tc != null) {
				String[] commandNames = tc.value();
				if (commandNames == null || commandNames.length == 0) {
					// calculate command-name out of the method-name/class-name
					if (method == null) {
						commandNames = new String[] { calculateCommandName(type) };
					} else {
						commandNames = new String[] { calculateCommandName(method) };
					}
				}
				for (String command : commandNames) {
					command = command.toLowerCase();
					if (!commands.containsKey(command)) {
						commands.put(command, new HashMap<Class<?>, Command>());
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
	 * @param type
	 *            Class to calculate the name for the test-command from
	 * @return the calculated name for the test-command
	 */
	private String calculateCommandName(Class<?> type) {
		String name = type.getSimpleName();
		String prefix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_CLASS_PREFIX);
		String postfix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_CLASS_POSTFIX);
		name = StringUtils.removeStartIgnoreCase(name, prefix);
		name = StringUtils.removeEndIgnoreCase(name, postfix);
		return name;
	}

	/**
	 * Calculate the name for the test-command out of the name of the given method. If there is a prefix and/or postfix
	 * configured, this will be removed from the name.
	 * 
	 * @param m
	 *            Method to calculate the name for the test-command from
	 * @return the calculated name for the test-command
	 */
	private String calculateCommandName(Method m) {
		String name = m.getName();
		String prefix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_METHOD_PREFIX);
		String postfix = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.COMMAND_METHOD_POSTFIX);
		name = StringUtils.removeStartIgnoreCase(name, prefix);
		name = StringUtils.removeEndIgnoreCase(name, postfix);
		return name;
	}

	/**
	 * Check, if the given class is a valid test-command implementation. A class is a valid test-command implementation,
	 * if there is only a single public method (functional interface!).
	 * 
	 * @param clazz
	 *            Class to check
	 * @return true, if the class defines exactly one single public method, else false
	 */
	private boolean checkTestCommandClass(Class<?> clazz) {
		// check number of public methods
		Method[] methods = clazz.getDeclaredMethods();
		int numberOfPublicMethods = 0;
		for (Method m : methods) {
			if (Modifier.isPublic(m.getModifiers())) {
				numberOfPublicMethods++;
			}
		}

		if (numberOfPublicMethods != 1) {
			return false;
		}

		return true;
	}

	/**
	 * Get the Command for the given command-name and type.
	 * 
	 * @param command
	 *            the excel-command
	 * @param clazz
	 *            the type of the test-class
	 * 
	 * @return the command for the given class, if found, else null
	 */
	public static Command getTestCommand(String command, Class<?> clazz) {
		if (commands.containsKey(command)) {
			Map<Class<?>, Command> cmds = commands.get(command);

			if (cmds != null && !cmds.isEmpty()) {
				if (clazz == null) {
					if (cmds.containsKey(clazz)) {
						return cmds.get(clazz);
					}
				} else {
					// not found? check superclass
					Class<?> cls = clazz;
					do {
						if (cmds.containsKey(cls)) {
							Command c = cmds.get(cls);
							if (c.getType() == Type.CLASS || c.getImplementation() == cls) {
								return c;
							}
						}
					} while ((cls = cls.getSuperclass()) != Object.class);
				}
			}
			return cmds.get(null);
		}
		return null;
	}

	/**
	 * Checks if the passed command exists
	 */
	public static boolean isTestCommandValid(String s) {
        if (commands.containsKey(s)) {
            return true;
        }

        boolean isDefaultCommand = false;
        for (DefaultCommands defaultCommands : DefaultCommands.values()) {
            isDefaultCommand = defaultCommands.getDefaultValue().equals(s);
            if(isDefaultCommand)
                break;
        }
        return isDefaultCommand;
    }
}
