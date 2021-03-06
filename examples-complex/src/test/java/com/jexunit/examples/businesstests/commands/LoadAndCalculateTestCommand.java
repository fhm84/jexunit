package com.jexunit.examples.businesstests.commands;

import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.annotation.TestParam;
import com.jexunit.core.context.Context;
import com.jexunit.core.context.TestContext;
import com.jexunit.examples.businesstests.boundary.MyComplexBusinessService;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * This is an implementation of multiple test-commands. The commands are: LOAD and CALCULATE.
 *
 * @author fabian
 */
public class LoadAndCalculateTestCommand {

    /**
     * Command-Implementation for the command LOAD. This will load the Entity by given id (in the Excel-File) and add it
     * to the TestContext (to use it later in other commands). This will also test the new feature of java 8: get the
     * parameter-names via reflection. The <code>&#64;TestParam</code>-Annotation has not set the value-attribute! The
     * name of the test-parameter to "inject" will be the same as the parameter-name.
     *
     * @param id      the id of the entity to load out of the test-context (this will be "injected" from the test-case)
     * @param context the TestContext to add the loaded entity to
     */
    @TestCommand
    public void load(@TestParam final long id, final TestContext context) {
        System.out.println("Load entity by id: " + id);

        final MyComplexBusinessService service = MyComplexBusinessService.getInstance();
        final MyComplexBusinessEntity entity = service.loadById(id);

        System.out.println("Entity: " + entity + ". Now we put it to the context.");

        context.add(MyComplexBusinessEntity.class, entity);
    }

    /**
     * Command-Implementation for the command CALCULATE. This will use the latest entity out of the TestContext and run
     * the business-calculation.
     *
     * @param entity the entity out of the TestContext
     */
    @TestCommand
    public void calculate(@Context final MyComplexBusinessEntity entity) {
        System.out.println("Entity from context: " + entity);

        final MyComplexBusinessService service = MyComplexBusinessService.getInstance();
        service.calculate(entity);
    }

}
