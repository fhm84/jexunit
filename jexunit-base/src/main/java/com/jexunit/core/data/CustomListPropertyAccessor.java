package com.jexunit.core.data;

import ognl.ListPropertyAccessor;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.List;
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

    /**
     * The maximum number of elements added to a list at once (for accessing an index beyond its size).
     */
    static final int MAX_NEW_ELEMENTS = 10_000;

    @Override
    public Object getProperty(final OgnlContext context, final Object target, final Object name)
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
                throw new NoSuchCollectionElementException(
                        PropertyTypes.getCollectionPropertyName(context.getCurrentNode()), propertyName);
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
                checkNewElements(list, index);
                final Class<?> elementType = getElementType(context);
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
    public void setProperty(final OgnlContext context, final Object target, final Object name,
                            final Object value) throws OgnlException {
        if (name instanceof Number) {
            @SuppressWarnings("unchecked") final List<Object> list = (List<Object>) target;
            final int index = ((Number) name).intValue();
            final Class<?> elementType = getElementType(context);

            final Object element = elementType == null || value == null
                    ? value
                    : context.getTypeConverter().convertValue(context, target, null, null, value, elementType);
            checkNewElements(list, index);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, element);
            return;
        }
        super.setProperty(context, target, name, value);
    }

    /**
     * Check the number of elements to add to the list for the given index. A (mistyped) huge index in the excel file
     * would otherwise fill the memory with <code>null</code> elements.
     *
     * @param list  the list
     * @param index the index to access
     * @throws OgnlException if more than {@link #MAX_NEW_ELEMENTS} elements would be added
     */
    private static void checkNewElements(final List<?> list, final int index) throws OgnlException {
        if (index - list.size() >= MAX_NEW_ELEMENTS) {
            throw new OgnlException(String.format(
                    "Index %d is too large: the list (size %d) is extended by at most %d elements at once", index,
                    list.size(), MAX_NEW_ELEMENTS));
        }
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
