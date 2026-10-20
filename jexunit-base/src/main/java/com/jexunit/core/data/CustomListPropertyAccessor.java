package com.jexunit.core.data;

import ognl.ListPropertyAccessor;
import ognl.Node;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom extension to ListPropertyAccessor that uses numbers and dynamic subscripts as properties to index into Lists.
 * This will add new element(s) to a list doesn't contain the element at the requested index.
 *
 * @author fabian
 */
public class CustomListPropertyAccessor extends ListPropertyAccessor {

    private static final Logger log = Logger.getLogger(CustomListPropertyAccessor.class.getName());

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
                final Class<?> elementType = getElementType((OgnlContext) context);
                for (int i = list.size(); i <= index; i++) {
                    list.add(createNewInstance(elementType));
                }
                return list.get(index);
            }
        }

        return super.getProperty(context, target, name);
    }

    /**
     * Set the element at the given index (like <code>tags[0]</code>). If the list is too small, it is filled up with
     * <code>null</code> elements. The value is converted to the element type of the list (if known).
     */
    @Override
    public void setProperty(@SuppressWarnings("rawtypes") final Map context, final Object target, final Object name,
                            final Object value) throws OgnlException {
        if (name instanceof Number) {
            @SuppressWarnings("unchecked") final List<Object> list = (List<Object>) target;
            final int index = ((Number) name).intValue();
            final OgnlContext ctx = (OgnlContext) context;
            final Class<?> elementType = getElementType(ctx);

            final Object element = elementType == null || value == null
                    ? value
                    : ctx.getTypeConverter().convertValue(context, target, null, null, value, elementType);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, element);
            return;
        }
        super.setProperty(context, target, name, value);
    }

    /**
     * Get the element type of the list accessed by the current (index) node, resolved from the type of the root object.
     *
     * @param ctx the OGNL context
     * @return the element type or <code>null</code>, if it cannot be resolved
     */
    private Class<?> getElementType(final OgnlContext ctx) {
        final Object root = ctx.getRoot();
        return root == null ? null : PropertyTypes.resolveElementType(root.getClass(), ctx.getCurrentNode());
    }

    private Object createNewInstance(final Class<?> elementType) {
        if (elementType == null) {
            return null;
        }
        try {
            return elementType.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            // e.g. no no-arg constructor (like Integer): the element has to be set directly (list[0]=value)
            log.log(Level.FINE, "Cannot create a new list element of type " + elementType.getName(), e);
            return null;
        }
    }

}
