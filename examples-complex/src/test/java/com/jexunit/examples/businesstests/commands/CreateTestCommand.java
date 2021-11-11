package com.jexunit.examples.businesstests.commands;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.examples.businesstests.boundary.MyComplexBusinessService;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * Test-Command implementation. This implements the test-command: CREATE. This command creates a new
 * entity out of the values set in the excel-file.
 *
 * @author fabian
 */
public class CreateTestCommand {

    @TestCommand("create")
    public void runCommand(MyComplexBusinessEntity entity) {
        final MyComplexBusinessService service = MyComplexBusinessService.getInstance();
        entity = service.save(entity);
    }

}
