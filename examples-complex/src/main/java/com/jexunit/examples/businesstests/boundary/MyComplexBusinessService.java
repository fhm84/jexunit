package com.jexunit.examples.businesstests.boundary;

import java.util.HashMap;
import java.util.Map;

import com.jexunit.examples.businesstests.control.MyComplexBusinessController;
import com.jexunit.examples.businesstests.entity.MyComplexBusinessEntity;

/**
 * This is the "very complex" business service of this example. <br>
 * This is implemented as singleton to "simulate" the storage of some elements (for example in a
 * database, ...).
 * 
 * @author fabian
 * 
 */
public class MyComplexBusinessService {

	private static MyComplexBusinessService instance = null;

	private MyComplexBusinessController controller = new MyComplexBusinessController();

	private Map<Long, MyComplexBusinessEntity> entities = new HashMap<>();
	private long idCount = 0l;

	public static MyComplexBusinessService getInstance() {
		if (instance == null) {
			instance = new MyComplexBusinessService();
		}
		return instance;
	}

	public MyComplexBusinessEntity save(MyComplexBusinessEntity entity) {
		if (entity.getId() == 0) {
			entity.setId(++idCount);
		}
		entities.put(entity.getId(), entity);
		return entity;
	}

	public MyComplexBusinessEntity loadById(long id) {
		return entities.get(id);
	}

	public void calculate(MyComplexBusinessEntity entity) {
		controller.calculate(entity);
	}
}
