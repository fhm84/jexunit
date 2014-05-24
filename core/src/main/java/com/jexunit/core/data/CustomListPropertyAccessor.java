package com.jexunit.core.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ognl.DynamicSubscript;
import ognl.ListPropertyAccessor;
import ognl.NoSuchPropertyException;
import ognl.Node;
import ognl.OgnlContext;
import ognl.OgnlException;

/**
 * Custom extension to ListPropertyAccessor that uses numbers and dynamic subscripts as properties
 * to index into Lists. This will add new element(s) to a list doesn't contain the element at the
 * requested index.
 *
 * @author fabian
 */
public class CustomListPropertyAccessor extends ListPropertyAccessor {

	public Object getProperty(@SuppressWarnings("rawtypes") Map context, Object target, Object name)
			throws OgnlException {
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) target;

		if (name instanceof String) {
			Object result = null;

			if (name.equals("size")) {
				result = new Integer(list.size());
			} else {
				if (name.equals("iterator")) {
					result = list.iterator();
				} else {
					if (name.equals("isEmpty") || name.equals("empty")) {
						result = list.isEmpty() ? Boolean.TRUE : Boolean.FALSE;
					} else {
						result = super.getProperty(context, target, name);
					}
				}
			}

			return result;
		}

		if (name instanceof Number) {
			int index = ((Number) name).intValue();
			if (index < list.size()) {
				return list.get(index);
			} else {
				// here we have to add new elements to the list!
				OgnlContext ctx = (OgnlContext) context;
				Node currentNode = ctx.getCurrentNode().jjtGetParent().jjtGetParent();
				for (int i = list.size(); i <= index; i++) {
					Object instance = createNewInstance(ctx.getCurrentType(), currentNode
							.jjtGetChild(0).toString());
					list.add(instance);
				}
				return list.get(index);
			}
		}

		if (name instanceof DynamicSubscript) {
			int len = list.size();
			switch (((DynamicSubscript) name).getFlag()) {
			case DynamicSubscript.FIRST:
				return len > 0 ? list.get(0) : null;
			case DynamicSubscript.MID:
				return len > 0 ? list.get(len / 2) : null;
			case DynamicSubscript.LAST:
				return len > 0 ? list.get(len - 1) : null;
			case DynamicSubscript.ALL:
				return new ArrayList<Object>(list);
			}
		}

		throw new NoSuchPropertyException(target, name);
	}

	private Object createNewInstance(Class<?> type, String fieldname) {
		try {
			Field listField = type.getDeclaredField(fieldname);
			ParameterizedType listType = (ParameterizedType) listField.getGenericType();
			Class<?> clazz = (Class<?>) listType.getActualTypeArguments()[0];
			return clazz.newInstance();
		} catch (NoSuchFieldException | SecurityException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
