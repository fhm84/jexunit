package com.jexunit.core.data;

import ognl.OgnlException;
import ognl.SetPropertyAccessor;

import java.util.Map;
import java.util.Set;

/**
 * Custom extension to ListPropertyAccessor that uses numbers and dynamic subscripts as properties to index into Lists.
 * This will add new element(s) to a list doesn't contain the element at the requested index.
 *
 * @author fabian
 */
public class CustomSetPropertyAccessor extends SetPropertyAccessor {

    @Override
    public Object getProperty(@SuppressWarnings("rawtypes") final Map context, final Object target, final Object name)
            throws OgnlException {
        @SuppressWarnings("unchecked") final Set<Object> set = (Set<Object>) target;

        if (name instanceof String) {
            // check for a condition (to to things like: set[name=John].count)
            final String propertyName = (String) name;

            final Object result;
            if (CollectionPropertyHelper.matches(propertyName)) {
                result = CollectionPropertyHelper.getProperty(context, set, propertyName);
            } else {
                result = super.getProperty(context, target, name);
            }

            return result;
        }

        return super.getProperty(context, target, name);
    }

}
