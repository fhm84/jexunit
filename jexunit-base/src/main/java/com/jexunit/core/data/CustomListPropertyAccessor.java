package com.jexunit.core.data;

import ognl.ListPropertyAccessor;
import ognl.Node;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

/**
 * Custom extension to ListPropertyAccessor that uses numbers and dynamic subscripts as properties to index into Lists.
 * This will add new element(s) to a list doesn't contain the element at the requested index.
 *
 * @author fabian
 */
public class CustomListPropertyAccessor extends ListPropertyAccessor {

    @Override
    public Object getProperty(@SuppressWarnings("rawtypes") final Map context, final Object target, final Object name)
            throws OgnlException {
        @SuppressWarnings("unchecked") final List<Object> list = (List<Object>) target;

        if (name instanceof String) {
            // check for a condition (to to things like: list[name=John].count)
            final String propertyName = (String) name;

            final Object result;
            if (CollectionPropertyHelper.matches(propertyName)) {
                result = CollectionPropertyHelper.getProperty(context, list, propertyName);
            } else {
                result = super.getProperty(context, target, name);
            }

            if (result == null) {
                final OgnlContext ctx = (OgnlContext) context;
                final Node currentNode = ctx.getCurrentNode().jjtGetParent().jjtGetParent();
                throw new NoSuchCollectionElementException(currentNode.jjtGetChild(0).toString(), propertyName);
            } else {
                return result;
            }
        }

        if (name instanceof Number) {
            final int index = ((Number) name).intValue();
            if (index < list.size()) {
                return list.get(index);
            } else {
                // here we have to add new elements to the list!
                final OgnlContext ctx = (OgnlContext) context;
                final Node currentNode = ctx.getCurrentNode().jjtGetParent().jjtGetParent();
                for (int i = list.size(); i <= index; i++) {
                    final Object instance = createNewInstance(ctx.getCurrentType(), currentNode.jjtGetChild(0).toString());
                    list.add(instance);
                }
                return list.get(index);
            }
        }

        return super.getProperty(context, target, name);
    }

    private Object createNewInstance(final Class<?> type, final String fieldname) {
        try {
            final Field listField = type.getDeclaredField(fieldname);
            final ParameterizedType listType = (ParameterizedType) listField.getGenericType();
            final Class<?> clazz = (Class<?>) listType.getActualTypeArguments()[0];
            return clazz.newInstance();
        } catch (final NoSuchFieldException | SecurityException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
