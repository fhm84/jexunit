package com.jexunit.core.data;

import ognl.NullHandler;
import ognl.Ognl;
import ognl.OgnlRuntime;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom NullHandler implementation for ognl. In JExUnit we do not know anything of the users model to test, so we have
 * to create new sub-entities if there should be set an attribute of these sub-entities. In the excel-test-file we do
 * not know if the sub-entities are already instantiated, so this NullHandler implementation will do this.
 *
 * @author fabian
 */
public class InstantiatingNullHandler implements NullHandler {

    private static final Logger log = Logger.getLogger(InstantiatingNullHandler.class.getName());

    /*
     * (non-Javadoc)
     *
     * @see ognl.NullHandler#nullMethodResult(java.util.Map, java.lang.Object, java.lang.String, java.lang.Object[])
     */
    @Override
    public Object nullMethodResult(@SuppressWarnings("rawtypes") final Map context, final Object target, final String methodName,
                                   final Object[] args) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see ognl.NullHandler#nullPropertyValue(java.util.Map, java.lang.Object, java.lang.Object)
     */
    @Override
    public Object nullPropertyValue(@SuppressWarnings("rawtypes") final Map context, final Object target, final Object property) {
        if (target == null || property == null) {
            return null;
        }

        try {
            final String propName = property.toString();

            final PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(target.getClass(), propName);
            if (pd == null) {
                return null;
            }

            final Class<?> clazz = pd.getPropertyType();

            if (clazz == null) {
                // can't do much here!
                return null;
            }

            final Object param = createObject(clazz);

            Ognl.setValue(propName, context, target, param);

            return param;
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Could not create and/or set value back on to object", e);
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    private <T> Object createObject(final Class<T> clazz) {
        if (Collection.class.isAssignableFrom(clazz)) {
            return new ArrayList<T>();
        } else if (clazz == Map.class) {
            return new HashMap();
        }

        try {
            return clazz.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

}
