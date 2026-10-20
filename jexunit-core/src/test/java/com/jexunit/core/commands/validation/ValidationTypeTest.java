package com.jexunit.core.commands.validation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ValidationTypeTest {

    @Test
    public void shouldParseValuesIgnoringCase() {
        assertEquals(ValidationType.WARN, ValidationType.parse("WARN"));
        assertEquals(ValidationType.FAIL, ValidationType.parse("fail"));
        assertEquals(ValidationType.IGNORE, ValidationType.parse(" Ignore "));
    }

    @Test
    public void shouldRejectInvalidValuesWithAllowedValues() {
        final IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> ValidationType.parse("NONE"));

        assertThat(e.getMessage(), containsString("[IGNORE, WARN, FAIL]"));
    }

}
