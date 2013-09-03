/**
 * 
 */
package com.jexunit.core.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;

import com.jexunit.core.GevoTester;

import eu.infomas.annotation.AnnotationDetector.MethodReporter;

/**
 * MethodReporter-Implementation for "storing" the annotated methods found. The "Annotation-Scan"
 * will run once, so we can hold the methods found in the static methods-Map.
 * 
 * TODO: "override" command-methods (what about different parameter-types?)
 * 
 * @author fabian
 * 
 */
public class TestCommandMethodScanner implements MethodReporter {

	private static final Map<String, Map<Class<?>, Method>> methods = new HashMap<>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.infomas.annotation.AnnotationDetector.Reporter#annotations()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Annotation>[] annotations() {
		return new Class[] { TestCommand.class };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.infomas.annotation.AnnotationDetector.MethodReporter#reportMethodAnnotation(java.lang.
	 * Class, java.lang.String, java.lang.String)
	 */
	@Override
	public void reportMethodAnnotation(Class<? extends Annotation> annotation, String className,
			String methodName) {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(className);
			Class<?> type = null;
			if (clazz.isAnnotationPresent(RunWith.class)) {
				RunWith rwa = clazz.getAnnotation(RunWith.class);
				if (rwa.value() == GevoTester.class) {
					type = clazz;
				}
			}
			if (annotation.isAnnotation() && annotation == TestCommand.class) {
				for (Method m : clazz.getDeclaredMethods()) {
					TestCommand tc = m.getAnnotation(TestCommand.class);
					if (tc != null) {
						for (String command : tc.value()) {
							command = command.toLowerCase();
							if (methods.containsKey(command)) {
								methods.get(command).put(type, m);
							} else {
								Map<Class<?>, Method> map = new HashMap<>();
								map.put(type, m);
								methods.put(command, map);
							}
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the command-method for the given command and type.
	 * 
	 * @param command
	 *            the excel-command
	 * @param clazz
	 *            the type of the test-class
	 * 
	 * @return the method for the given class, if found, else null
	 */
	public static Method getTestCommandMethod(String command, Class<?> clazz) {
		if (methods.containsKey(command)) {
			Map<Class<?>, Method> map = methods.get(command);
			if (map.containsKey(clazz)) {
				return map.get(clazz);
			} else if (clazz != null && !map.containsKey(clazz)) {
				Class<?> c = clazz;
				// is there a command-method defined for the test-class?
				if (map.containsKey(c)) {
					return map.get(c);
				} else {
					// or is there a command-method defined for a baseClass of the given type?
					while ((c = c.getSuperclass()) != Object.class) {
						if (map.containsKey(c)) {
							return map.get(c);
						}
					}
				}
			}
			return map.get(null);
		} else {
			return null;
		}
	}
}
