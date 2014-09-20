package com.jexunit.core.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import ognl.ListPropertyAccessor;
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

	@Override
	public Object getProperty(@SuppressWarnings("rawtypes") Map context, Object target, Object name)
			throws OgnlException {
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) target;

		if (name instanceof String) {
			Object result = null;

			// check for a condition (to to things like: list[name=John].count)
			String propertyName = (String) name;
			if (CollectionPropertyHelper.matches(propertyName)) {
				result = CollectionPropertyHelper.getProperty(context, list, propertyName);
			} else {
				result = super.getProperty(context, target, name);
			}

			if (result == null) {
				OgnlContext ctx = (OgnlContext) context;
				Node currentNode = ctx.getCurrentNode().jjtGetParent().jjtGetParent();
				throw new NoSuchCollectionElementException(currentNode.jjtGetChild(0).toString(),
						propertyName);
			} else {
				return result;
			}
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

		return super.getProperty(context, target, name);
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
