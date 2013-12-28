package com.jexunit.examples.businesstests.commands;

import java.util.Map;

import com.jexunit.core.commands.TestCommand;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import com.jexunit.examples.businesstests.boundary.MyComplexBusinessService;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * @author fabian
 * 
 */
public class LoadAndCalculateTestCommand {

	@TestCommand("load")
	public void load(TestCase testCase, TestContext context) {
		Map<String, TestCell> values = testCase.getValues();
		TestCell idCell = values.get("id");
		long id = ((Double) Double.parseDouble(idCell.getValue())).longValue();

		System.out.println("Load entity by id: " + id);

		MyComplexBusinessService service = MyComplexBusinessService.getInstance();
		MyComplexBusinessEntity entity = service.loadById(id);

		System.out.println("Entity: " + entity + ". Now we put it to the context.");

		context.add(MyComplexBusinessEntity.class, entity);
	}

	@TestCommand("calculate")
	public void calculate(@Context MyComplexBusinessEntity entity) {
		System.out.println("Entity from context: " + entity);

		MyComplexBusinessService service = MyComplexBusinessService.getInstance();
		service.calculate(entity);
	}
}
