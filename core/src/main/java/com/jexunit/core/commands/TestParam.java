package com.jexunit.core.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method parameter to lookup out of the current test-case. This allows you to
 * "inject" a parameter out of the excel-file by name/id directly into your test-command
 * implementation.
 * <p>
 * Example: to "inject" the id parameter (set in the excel-file), you can do something like this:<br>
 * <code>
 * &#64;TestCommand("myCommand")<br>public void runCommand(&#64;TestParam("id") long id) { ... }
 * </code>
 * </p>
 * In this case you don't have to "inject" the whole TestCase to get the id.
 * 
 * @author fabian
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface TestParam {

	/**
	 * Get the name/id of the of the value to lookup out of the test-parameters.
	 * 
	 * @return the name/id of the value to lookup
	 */
	String value() default "";

	/**
	 * Mark the parameter as required. If there is a parameter marked as required but is null, the
	 * framework will not invoke the method and the test will fail!<br>
	 * Default value is true.
	 * 
	 * @return true, if the parameter is required (default), else false
	 */
	boolean required() default true;
}