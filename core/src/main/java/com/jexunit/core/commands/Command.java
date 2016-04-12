package com.jexunit.core.commands;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Container object representing a test command containing the command name, the class it is defined in, the type of the
 * command (class or method), ...
 * 
 * @author fabian
 *
 */
public class Command {

	private String name;
	private Type type;

	/**
	 * the method implementing the test command (if type METHOD) or null if the implementation is of type CLASS.
	 */
	private Method method;
	/**
	 * the type, the test command is defined in (if type METHOD and method is not null) or the type implementing the
	 * test command
	 */
	private Class<?> implementation;

	/**
	 * the default fastFail value for the command. the fast fail flag will immediately fail the complete test group if
	 * the command fails.
	 */
	private boolean fastFail = false;

	public static enum Type {
		CLASS, METHOD;
	}

	public Command(String name, Class<?> implementation) {
		this(name, implementation, null, false);
	}

	public Command(String name, Class<?> implementation, boolean fastFail) {
		this(name, implementation, null, fastFail);
	}

	public Command(String name, Class<?> implementation, Method method) {
		this(name, implementation, method, false);
	}

	public Command(String name, Class<?> implementation, Method method, boolean fastFail) {
		this.name = name;
		this.implementation = implementation;
		this.method = method;
		if (method == null) {
			this.type = Type.CLASS;
		} else {
			this.type = Type.METHOD;
		}
		this.fastFail = fastFail;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public boolean isStaticMethod() {
		if (method != null) {
			return Modifier.isStatic(method.getModifiers());
		}
		return false;
	}

	public Class<?> getImplementation() {
		return implementation;
	}

	public void setImplementation(Class<?> implementation) {
		this.implementation = implementation;
	}

	public boolean isFastFail() {
		return fastFail;
	}

	public void setFastFail(boolean fastFail) {
		this.fastFail = fastFail;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Command)) {
			return false;
		}
		Command c = (Command) o;
		return new EqualsBuilder().append(name, c.name).append(type, c.type).append(method, c.method)
				.append(implementation, c.implementation).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(name).append(type).append(method).append(implementation).hashCode();
	}

	// TODO: add "mapping" details to set the method parameters, "inject" the test params, ...
}
