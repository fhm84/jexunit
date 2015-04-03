package com.jexunit.core.data;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ognl.NullHandler;
import ognl.Ognl;
import ognl.OgnlRuntime;

/**
 * Custom NullHandler implementation for ognl. In JExUnit we do not know anything of the users model to test, so we have
 * to create new sub-entities if there should be set an attribute of these sub-entities. In the excel-test-file we do
 * not know if the sub-entities are already instantiated, so this NullHandler implementation will do this.
 * 
 * @author fabian
 *
 */
public class InstantiatingNullHandler implements NullHandler {

	private static Logger log = Logger.getLogger(InstantiatingNullHandler.class.getName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see ognl.NullHandler#nullMethodResult(java.util.Map, java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object nullMethodResult(@SuppressWarnings("rawtypes") Map context, Object target, String methodName,
			Object[] args) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ognl.NullHandler#nullPropertyValue(java.util.Map, java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object nullPropertyValue(@SuppressWarnings("rawtypes") Map context, Object target, Object property) {
		if (target == null || property == null) {
			return null;
		}

		try {
			String propName = property.toString();
			Class<?> clazz = null;

			if (target != null) {
				PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(target.getClass(), propName);
				if (pd == null) {
					return null;
				}

				clazz = pd.getPropertyType();
			}

			if (clazz == null) {
				// can't do much here!
				return null;
			}

			Object param = createObject(clazz);

			Ognl.setValue(propName, context, target, param);

			return param;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not create and/or set value back on to object", e);
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	private <T> Object createObject(Class<T> clazz) {
		if (Collection.class.isAssignableFrom(clazz)) {
			return new ArrayList<T>();
		} else if (clazz == Map.class) {
			return new HashMap();
		}

		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
