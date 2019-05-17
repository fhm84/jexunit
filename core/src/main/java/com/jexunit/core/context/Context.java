package com.jexunit.core.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking the TestContext to "inject" (into the test-command-method).
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
