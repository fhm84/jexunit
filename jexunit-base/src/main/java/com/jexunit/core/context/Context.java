package com.jexunit.core.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for "injecting" a value out of the TestContext: into a parameter of a test-command method, or into a field
 * of a test-command class (<code>&#64;TestCommand</code> on the class; filled for each invocation, also inherited
 * fields). Without a value, the value is looked up by its type.
 *
 * @author fabian
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Context {

    /**
     * The id for lookup the value in the context.
     *
     * @return the id for lookup the value
     */
    String value() default "";

}
