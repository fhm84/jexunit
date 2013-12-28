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
 * This is an implementation of multiple test-commands. The commands are: LOAD and CALCULATE.
 * 
 * @author fabian
 * 
 */
public class LoadAndCalculateTestCommand {

	/**
	 * Command-Implementation for the command LOAD. This will load the Entity by given id (in the
	 * Excel-File) and add it to the TestContext (to use it later in other commands).
	 * 
	 * @param testCase
	 *            the TestCase (with the id of the entity to load)
	 * @param context
	 *            the TestContext to add the loaded entity to
	 */
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

	/**
	 * Command-Implementation for the command CALCULATE. This will use the latest entity out of the
	 * TestContext and run the business-calculation.
	 * 
	 * @param entity
	 *            the entity out of the TestContext
	 */
	@TestCommand("calculate")
	public void calculate(@Context MyComplexBusinessEntity entity) {
		System.out.println("Entity from context: " + entity);

		MyComplexBusinessService service = MyComplexBusinessService.getInstance();
		service.calculate(entity);
	}
}
