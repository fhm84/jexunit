package com.jexunit.core.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for representing a method as the implementation for multiple test-commands.
 * 
 * @author fabian
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TestCommands {

	/**
	 * Get the list of TestCommans.
	 * 
	 * @return the list of the commands the annotated method implements
	 */
	TestCommand[] value();
}