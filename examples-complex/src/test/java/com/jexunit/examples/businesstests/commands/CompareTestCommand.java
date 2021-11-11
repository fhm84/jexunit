package com.jexunit.examples.businesstests.commands;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.context.Context;
import com.jexunit.core.data.TestObjectHelper;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test-Command implementation. This implements the command: COMPARE. This command will get the current entity out of
 * the TestContext and compare it with the properties out of the excel-file (TestCase). The JExUnit-Framework will be
 * used to "inject" the objects needed.
 *
 * @author fabian
 */
@TestCommand("compare")
public class CompareTestCommand {

    /**
     * This is an example implementation for a reusable compare test-command checking all the values set in the test
     * (excel-file).
     *
     * @param testCase the current testCase
     * @param actual   the current business entity
     * @throws Exception
     */
    public void compare(@Context final TestCase<?> testCase,
                        @Context final MyComplexBusinessEntity actual) throws Exception {
        for (final Map.Entry<String, TestCell> entry : testCase.getValues().entrySet()) {
            final Object obj = TestObjectHelper.getProperty(actual, entry.getKey());
            final Object expected = TestObjectHelper.convertPropertyStringToObject(obj.getClass(),
                    entry.getValue().getValue());
            if (obj instanceof BigDecimal && expected instanceof BigDecimal) {
                assertThat(((BigDecimal) obj).compareTo((BigDecimal) expected), is(0));
            } else {
                assertThat(obj, is(equalTo(expected)));
            }
        }
    }

}
