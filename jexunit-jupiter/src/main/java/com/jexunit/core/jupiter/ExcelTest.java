package com.jexunit.core.jupiter;

import org.junit.jupiter.api.TestTemplate;

import java.lang.annotation.*;

/**
 * Marks the single template method that JExUnitExtension uses as the entry point for running
 * Excel-defined test cases. Place exactly one {@code @ExcelTest} method in a test class that is
 * annotated with {@code @ExtendWith(JExUnitExtension.class)}.
 *
 * <p>The method body is ignored; the extension intercepts the invocation and runs the command loop.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestTemplate
public @interface ExcelTest {
}
