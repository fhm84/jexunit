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

		if (entity.getAdditionalSettings() != null) {
			String sex = entity.getAdditionalSettings().get("sex");
			if (sex != null && "F".equalsIgnoreCase(sex)) {
				result3 = result3.multiply(new BigDecimal("0.98"));
			}
		}

		entity.setCalcField1(result1);
		entity.setCalcField2(result2);
		entity.setCalcField3(result3);

		MyComplexBusinessEntity mcbe1 = new MyComplexBusinessEntity();
		mcbe1.setId(25);
		mcbe1.setName("John");
		mcbe1.setCount(13);
		entity.getList().add(mcbe1);

		MyComplexBusinessEntity mcbe2 = new MyComplexBusinessEntity();
		mcbe2.setId(28);
		mcbe2.setName("Doe");
		mcbe2.setCount(100);
		entity.getList().add(mcbe2);
	}
}
