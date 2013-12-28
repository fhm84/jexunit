package com.jexunit.examples.businesstests.commands;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.RoundingMode;

import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.context.Context;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * @author fabian
 * 
 */
public class CompareTestCommand {

	@TestCommand("compare")
	public void compare(@Context MyComplexBusinessEntity actual, MyComplexBusinessEntity expected) {
		if (expected.getId() != 0) {
			assertThat(actual.getId(), is(equalTo(expected.getId())));
		}

		if (expected.getName() != null) {
			assertThat(actual.getName(), is(equalTo(expected.getName())));
		}

		if (expected.getCity() != null) {
			assertThat(actual.getCity(), is(equalTo(expected.getCity())));
		}

		if (expected.getBirthday() != null) {
			assertThat(actual.getBirthday(), is(equalTo(expected.getBirthday())));
		}

		if (expected.getCount() != 0) {
			assertThat(actual.getCount(), is(equalTo(expected.getCount())));
		}

		if (expected.getRate() != 0) {
			assertThat(actual.getRate(), is(equalTo(expected.getRate())));
		}

		if (expected.getPercentage() != 0) {
			assertThat(actual.getPercentage(), is(equalTo(expected.getPercentage())));
		}

		if (expected.getCalcField1() != null) {
			assertThat(actual.getCalcField1().setScale(2, RoundingMode.HALF_UP), is(expected
					.getCalcField1().setScale(2, RoundingMode.HALF_UP)));
		}

		if (expected.getCalcField2() != null) {
			assertThat(actual.getCalcField2(), is(expected.getCalcField2()));
		}

		if (expected.getCalcField3() != null) {
			assertThat(actual.getCalcField3(), is(expected.getCalcField3()));
		}
	}
}
