package com.jexunit.examples.businesstests.control;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * This is the controller class for running the "very complex" calculations ;).
 * 
 * @author fabian
 * 
 */
public class MyComplexBusinessController {

	private static final BigDecimal myConst = new BigDecimal("0.19");

	public void calculate(MyComplexBusinessEntity entity) {
		BigDecimal result1 = new BigDecimal(entity.getCount() * entity.getRate());
		BigDecimal result2 = result1.divide(new BigDecimal(entity.getPercentage()), 4,
				RoundingMode.HALF_UP);
		BigDecimal result3 = result1.multiply(myConst).add(result2);

		entity.setCalcField1(result1);
		entity.setCalcField2(result2);
		entity.setCalcField3(result3);
	}
}
