package com.jexunit.examples.businesstests.commands;

import com.jexunit.core.commands.TestCommand;
import com.jexunit.examples.businesstests.boundary.MyComplexBusinessService;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * @author fabian
 * 
 */
public class CreateTestCommand {

	@TestCommand("create")
	public void runCommand(MyComplexBusinessEntity entity) {
		MyComplexBusinessService service = MyComplexBusinessService.getInstance();
		entity = service.save(entity);
	}
}
