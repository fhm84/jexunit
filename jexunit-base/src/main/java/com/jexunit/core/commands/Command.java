package com.jexunit.core.commands;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Container object representing a test command containing the command name, the class it is defined in, the type of the
 * command (class or method), ...
 *
 * @author fabian
 */
@Getter
@Setter
@EqualsAndHashCode
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
    private boolean fastFail;

    public static enum Type {
        CLASS, METHOD;
    }

    public Command(final String name, final Class<?> implementation) {
        this(name, implementation, null, false);
    }

    public Command(final String name, final Class<?> implementation, final boolean fastFail) {
        this(name, implementation, null, fastFail);
    }

    public Command(final String name, final Class<?> implementation, final Method method) {
        this(name, implementation, method, false);
    }

    public Command(final String name, final Class<?> implementation, final Method method, final boolean fastFail) {
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

    public boolean isStaticMethod() {
        return method != null && Modifier.isStatic(method.getModifiers());
    }

    // TODO: add "mapping" details to set the method parameters, "inject" the test params, ...
}
