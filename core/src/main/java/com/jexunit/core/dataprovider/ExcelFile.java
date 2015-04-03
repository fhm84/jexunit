package com.jexunit.core.dataprovider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the variable/method that provides the filename(s) for the excel-file(s) for the test.
 * 
 * @author fabian
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ExcelFile {

	boolean worksheetAsTest() default true;

}