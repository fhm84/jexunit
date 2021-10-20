package com.jexunit.core.dataprovider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the variable/method that provides the filename(s) for the excel-file(s) for the test.
 *
 * @author fabian
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ExcelFile {

    /**
     * Interpret the whole worksheet as one test case (<code>true</code>)
     * or each command as test case (<code>false</code>)
     *
     * @return <code>true</code> to interpret a whole worksheet as test case,
     * <code>false</code> to interpret each command as test case
     */
    boolean worksheetAsTest() default true;

    /**
     * Transpose (rotate) data from rows to columns. By default, commands are declared in rows, by setting this flag to
     * <code>true</code> commands will be declared in columns!
     *
     * @return <code>false</code> if commands are read from rows, <code>false</code> if commands are read from columns
     */
    boolean transpose() default false;

}