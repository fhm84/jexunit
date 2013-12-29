package com.jexunit.core.data;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.MappedPropertyDescriptor;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.WrapDynaBean;
import org.apache.commons.beanutils.expression.DefaultResolver;
import org.apache.commons.beanutils.expression.Resolver;

/**
 * This is copied from the apache commons PropertyUtils and modified for using in jexunit. So a not
 * existing (list-) element will be created automatically (if possible) to set nested properties.
 * <p>
 * Do not use this class directly! This should only be used inside the {@link TestObjectHelper}!
 * </p>
 * 
 * @author fabian
 * 
 */
class PropertyUtils {

	/** Log instance */
	private static Logger log = Logger.getLogger(PropertyUtils.class.getName());

	private Resolver resolver = new DefaultResolver();

	/**
	 * The cache of PropertyDescriptor arrays for beans we have already introspected, keyed by the
	 * java.lang.Class of this object.
	 */
	private static final Class<?>[] EMPTY_CLASS_PARAMETERS = new Class[0];
	private static final Class<?>[] LIST_CLASS_PARAMETER = new Class[] { java.util.List.class };

	/** An empty object array */
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	/**
	 * Return the value of the specified indexed property of the specified bean, with no type
	 * conversions. The zero-relative index of the required value must be included (in square
	 * brackets) as a suffix to the property name, or <code>IllegalArgumentException</code> will be
	 * thrown. In addition to supporting the JavaBeans specification, this method has been extended
	 * to support <code>List</code> objects as well.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            <code>propertyname[index]</code> of the property value to be extracted
	 * @return the indexed property value
	 * 
	 * @exception IndexOutOfBoundsException
	 *                if the specified index is outside the valid range for the underlying array or
	 *                List
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public Object getIndexedProperty(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Identify the index of the requested individual property
		int index = -1;
		try {
			index = resolver.getIndex(name);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid indexed property '" + name
					+ "' on bean class '" + bean.getClass() + "' " + e.getMessage());
		}
		if (index < 0) {
			throw new IllegalArgumentException("Invalid indexed property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}

		// Isolate the name
		name = resolver.getProperty(name);

		// Request the specified indexed property value
		return getIndexedProperty(bean, name, index);
	}

	/**
	 * Return the value of the specified indexed property of the specified bean, with no type
	 * conversions. In addition to supporting the JavaBeans specification, this method has been
	 * extended to support <code>List</code> objects as well.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Simple property name of the property value to be extracted
	 * @param index
	 *            Index of the property value to be extracted
	 * @return the indexed property value
	 * 
	 * @exception IndexOutOfBoundsException
	 *                if the specified index is outside the valid range for the underlying property
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public Object getIndexedProperty(Object bean, String name, int index)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null || name.length() == 0) {
			if (bean.getClass().isArray()) {
				return Array.get(bean, index);
			} else if (bean instanceof List) {
				return ((List<?>) bean).get(index);
			}
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Retrieve the property descriptor for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "' on bean class '"
					+ bean.getClass() + "'");
		}

		// Call the indexed getter method if there is one
		if (descriptor instanceof IndexedPropertyDescriptor) {
			Method readMethod = ((IndexedPropertyDescriptor) descriptor).getIndexedReadMethod();
			readMethod = MethodUtils.getAccessibleMethod(bean.getClass(), readMethod);
			if (readMethod != null) {
				Object[] subscript = new Object[1];
				subscript[0] = new Integer(index);
				try {
					return (invokeMethod(readMethod, bean, subscript));
				} catch (InvocationTargetException e) {
					if (e.getTargetException() instanceof IndexOutOfBoundsException) {
						throw (IndexOutOfBoundsException) e.getTargetException();
					} else {
						throw e;
					}
				}
			}
		}

		// Otherwise, the underlying property must be an array
		Method readMethod = getReadMethod(bean.getClass(), descriptor);
		if (readMethod == null) {
			throw new NoSuchMethodException("Property '" + name + "' has no "
					+ "getter method on bean class '" + bean.getClass() + "'");
		}

		// Call the property getter and return the value
		Object value = invokeMethod(readMethod, bean, EMPTY_OBJECT_ARRAY);
		if (value == null) {
			throw new IllegalArgumentException("Property '" + name
					+ "' results in null on bean class '" + bean.getClass()
					+ "'. Please initialize this property!");
		}
		if (!value.getClass().isArray()) {
			if (!(value instanceof java.util.List)) {
				throw new IllegalArgumentException("Property '" + name
						+ "' is not indexed on bean class '" + bean.getClass() + "'");
			} else {
				if (((java.util.List<?>) value).size() > index) {
					// get the List's value
					return ((java.util.List<?>) value).get(index);
				} else {
					Object val = null;
					try {
						ParameterizedType t = (ParameterizedType) bean.getClass()
								.getDeclaredField(name).getGenericType();
						Type type = t.getActualTypeArguments()[0];
						val = ((Class<?>) type).newInstance();
					} catch (NoSuchFieldException | SecurityException | InstantiationException e) {
						try {
							val = descriptor.getPropertyType().newInstance();
						} catch (InstantiationException ex) {
							val = new Object();
						}
					}
					((java.util.List<Object>) value).add(index, val);
					return val;
				}
			}
		} else {
			// get the array's value
			try {
				return (Array.get(value, index));
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: "
						+ Array.getLength(value) + " for property '" + name + "'");
			}
		}
	}

	/**
	 * Return the value of the specified mapped property of the specified bean, with no type
	 * conversions. The key of the required value must be included (in brackets) as a suffix to the
	 * property name, or <code>IllegalArgumentException</code> will be thrown.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            <code>propertyname(key)</code> of the property value to be extracted
	 * @return the mapped property value
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public Object getMappedProperty(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Identify the key of the requested individual property
		String key = null;
		try {
			key = resolver.getKey(name);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid mapped property '" + name
					+ "' on bean class '" + bean.getClass() + "' " + e.getMessage());
		}
		if (key == null) {
			throw new IllegalArgumentException("Invalid mapped property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}

		// Isolate the name
		name = resolver.getProperty(name);

		// Request the specified indexed property value
		return getMappedProperty(bean, name, key);
	}

	/**
	 * Return the value of the specified mapped property of the specified bean, with no type
	 * conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Mapped property name of the property value to be extracted
	 * @param key
	 *            Key of the property value to be extracted
	 * @return the mapped property value
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public Object getMappedProperty(Object bean, String name, String key)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}
		if (key == null) {
			throw new IllegalArgumentException("No key specified for property '" + name
					+ "' on bean class " + bean.getClass() + "'");
		}

		// Handle DynaBean instances specially
		if (bean instanceof DynaBean) {
			DynaProperty descriptor = ((DynaBean) bean).getDynaClass().getDynaProperty(name);
			if (descriptor == null) {
				throw new NoSuchMethodException("Unknown property '" + name + "'+ on bean class '"
						+ bean.getClass() + "'");
			}
			return (((DynaBean) bean).get(name, key));
		}

		Object result = null;

		// Retrieve the property descriptor for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "'+ on bean class '"
					+ bean.getClass() + "'");
		}

		if (descriptor instanceof MappedPropertyDescriptor) {
			// Call the keyed getter method if there is one
			Method readMethod = ((MappedPropertyDescriptor) descriptor).getMappedReadMethod();
			readMethod = MethodUtils.getAccessibleMethod(bean.getClass(), readMethod);
			if (readMethod != null) {
				Object[] keyArray = new Object[1];
				keyArray[0] = key;
				result = invokeMethod(readMethod, bean, keyArray);
			} else {
				throw new NoSuchMethodException("Property '" + name
						+ "' has no mapped getter method on bean class '" + bean.getClass() + "'");
			}
		} else {
			/* means that the result has to be retrieved from a map */
			Method readMethod = getReadMethod(bean.getClass(), descriptor);
			if (readMethod != null) {
				Object invokeResult = invokeMethod(readMethod, bean, EMPTY_OBJECT_ARRAY);
				/* test and fetch from the map */
				if (invokeResult instanceof java.util.Map) {
					result = ((java.util.Map<String, Object>) invokeResult).get(key);
				}
			} else {
				throw new NoSuchMethodException("Property '" + name
						+ "' has no mapped getter method on bean class '" + bean.getClass() + "'");
			}
		}
		return result;
	}

	/**
	 * Return the value of the (possibly nested) property of the specified name, for the specified
	 * bean, with no type conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Possibly nested name of the property to be extracted
	 * @return the nested property value
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception NestedNullException
	 *                if a nested reference to a property returns null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public Object getNestedProperty(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = null;
			if (bean instanceof Map) {
				nestedBean = getPropertyOfMapBean((Map<String, Object>) bean, next);
			} else if (resolver.isMapped(next)) {
				nestedBean = getMappedProperty(bean, next);
			} else if (resolver.isIndexed(next)) {
				nestedBean = getIndexedProperty(bean, next);
			} else {
				nestedBean = getSimpleProperty(bean, next);
			}
			if (nestedBean == null) {
				throw new NestedNullException("Null property value for '" + name
						+ "' on bean class '" + bean.getClass() + "'");
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		if (bean instanceof Map) {
			bean = getPropertyOfMapBean((Map<String, Object>) bean, name);
		} else if (resolver.isMapped(name)) {
			bean = getMappedProperty(bean, name);
		} else if (resolver.isIndexed(name)) {
			bean = getIndexedProperty(bean, name);
		} else {
			bean = getSimpleProperty(bean, name);
		}
		return bean;
	}

	/**
	 * This method is called by getNestedProperty and setNestedProperty to define what it means to
	 * get a property from an object which implements Map. See setPropertyOfMapBean for more
	 * information.
	 * 
	 * @param bean
	 *            Map bean
	 * @param propertyName
	 *            The property name
	 * @return the property value
	 * 
	 * @throws IllegalArgumentException
	 *             when the propertyName is regarded as being invalid.
	 * 
	 * @throws IllegalAccessException
	 *             just in case subclasses override this method to try to access real getter methods
	 *             and find permission is denied.
	 * 
	 * @throws InvocationTargetException
	 *             just in case subclasses override this method to try to access real getter
	 *             methods, and find it throws an exception when invoked.
	 * 
	 * @throws NoSuchMethodException
	 *             just in case subclasses override this method to try to access real getter
	 *             methods, and want to fail if no simple method is available.
	 * @since 1.8.0
	 */
	protected Object getPropertyOfMapBean(Map<String, Object> bean, String propertyName)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (resolver.isMapped(propertyName)) {
			String name = resolver.getProperty(propertyName);
			if (name == null || name.length() == 0) {
				propertyName = resolver.getKey(propertyName);
			}
		}

		if (resolver.isIndexed(propertyName) || resolver.isMapped(propertyName)) {
			throw new IllegalArgumentException("Indexed or mapped properties are not supported on"
					+ " objects of type Map: " + propertyName);
		}

		return bean.get(propertyName);
	}

	/**
	 * Return the value of the specified property of the specified bean, no matter which property
	 * reference format is used, with no type conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Possibly indexed and/or nested name of the property to be extracted
	 * @return the property value
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public Object getProperty(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		return getNestedProperty(bean, name);
	}

	/**
	 * <p>
	 * Retrieve the property descriptor for the specified property of the specified bean, or return
	 * <code>null</code> if there is no such descriptor. This method resolves indexed and nested
	 * property references in the same manner as other methods in this class, except that if the
	 * last (or only) name element is indexed, the descriptor for the last resolved property itself
	 * is returned.
	 * </p>
	 * 
	 * @param bean
	 *            Bean for which a property descriptor is requested
	 * @param name
	 *            Possibly indexed and/or nested name of the property for which a property
	 *            descriptor is requested
	 * @return the property descriptor
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception IllegalArgumentException
	 *                if a nested reference to a property returns null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public PropertyDescriptor getPropertyDescriptor(Object bean, String name)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = getProperty(bean, next);
			if (nestedBean == null) {
				throw new NestedNullException("Null property value for '" + next
						+ "' on bean class '" + bean.getClass() + "'");
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		// Remove any subscript from the final name value
		name = resolver.getProperty(name);

		// Look up and return this property from our cache
		// creating and adding it to the cache if not found.
		if (name == null) {
			return (null);
		}

		PropertyDescriptor[] descriptors = getPropertyDescriptors(bean);
		if (descriptors != null) {
			for (int i = 0; i < descriptors.length; i++) {
				if (name.equals(descriptors[i].getName())) {
					return descriptors[i];
				}
			}
		}

		return null;
	}

	/**
	 * <p>
	 * Retrieve the property descriptors for the specified class, introspecting and caching them the
	 * first time a particular bean class is encountered.
	 * </p>
	 * 
	 * @param beanClass
	 *            Bean class for which property descriptors are requested
	 * @return the property descriptors
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>beanClass</code> is null
	 */
	public PropertyDescriptor[] getPropertyDescriptors(Class<?> beanClass) {
		if (beanClass == null) {
			throw new IllegalArgumentException("No bean class specified");
		}

		// Look up any cached descriptors for this bean class
		PropertyDescriptor[] descriptors = null;

		// Introspect the bean and cache the generated descriptors
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo(beanClass);
		} catch (IntrospectionException e) {
			return (new PropertyDescriptor[0]);
		}
		descriptors = beanInfo.getPropertyDescriptors();
		if (descriptors == null) {
			descriptors = new PropertyDescriptor[0];
		}

		for (int i = 0; i < descriptors.length; i++) {
			if (descriptors[i] instanceof IndexedPropertyDescriptor) {
				IndexedPropertyDescriptor descriptor = (IndexedPropertyDescriptor) descriptors[i];
				String propName = descriptor.getName().substring(0, 1).toUpperCase()
						+ descriptor.getName().substring(1);

				if (descriptor.getReadMethod() == null) {
					String methodName = descriptor.getIndexedReadMethod() != null ? descriptor
							.getIndexedReadMethod().getName() : "get" + propName;
					Method readMethod = MethodUtils.getMatchingAccessibleMethod(beanClass,
							methodName, EMPTY_CLASS_PARAMETERS);
					if (readMethod != null) {
						try {
							descriptor.setReadMethod(readMethod);
						} catch (Exception e) {
							log.log(Level.SEVERE, "Error setting indexed property read method", e);
						}
					}
				}
				if (descriptor.getWriteMethod() == null) {
					String methodName = descriptor.getIndexedWriteMethod() != null ? descriptor
							.getIndexedWriteMethod().getName() : "set" + propName;
					Method writeMethod = MethodUtils.getMatchingAccessibleMethod(beanClass,
							methodName, LIST_CLASS_PARAMETER);
					if (writeMethod == null) {
						Method[] methods = beanClass.getMethods();
						for (int j = 0; j < methods.length; j++) {
							if (methods[j].getName().equals(methodName)) {
								Class<?>[] parameterTypes = methods[j].getParameterTypes();
								if (parameterTypes.length == 1
										&& List.class.isAssignableFrom(parameterTypes[0])) {
									writeMethod = methods[j];
									break;
								}
							}
						}
					}
					if (writeMethod != null) {
						try {
							descriptor.setWriteMethod(writeMethod);
						} catch (Exception e) {
							log.log(Level.SEVERE, "Error setting indexed property write method", e);
						}
					}
				}
			}
		}

		return descriptors;
	}

	/**
	 * <p>
	 * Retrieve the property descriptors for the specified bean, introspecting and caching them the
	 * first time a particular bean class is encountered.
	 * </p>
	 * 
	 * @param bean
	 *            Bean for which property descriptors are requested
	 * @return the property descriptors
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> is null
	 */
	public PropertyDescriptor[] getPropertyDescriptors(Object bean) {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		return getPropertyDescriptors(bean.getClass());
	}

	/**
	 * Return the Java Class representing the property type of the specified property, or
	 * <code>null</code> if there is no such property for the specified bean. This method follows
	 * the same name resolution rules used by <code>getPropertyDescriptor()</code>, so if the last
	 * element of a name reference is indexed, the type of the property itself will be returned. If
	 * the last (or only) element has no property with the specified name, <code>null</code> is
	 * returned.
	 * 
	 * @param bean
	 *            Bean for which a property descriptor is requested
	 * @param name
	 *            Possibly indexed and/or nested name of the property for which a property
	 *            descriptor is requested
	 * @return The property type
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception IllegalArgumentException
	 *                if a nested reference to a property returns null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public Class<?> getPropertyType(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = getProperty(bean, next);
			if (nestedBean == null) {
				throw new NestedNullException("Null property value for '" + next
						+ "' on bean class '" + bean.getClass() + "'");
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		// Remove any subscript from the final name value
		name = resolver.getProperty(name);

		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			return null;
		} else if (descriptor instanceof IndexedPropertyDescriptor) {
			return ((IndexedPropertyDescriptor) descriptor).getIndexedPropertyType();
		} else if (descriptor instanceof MappedPropertyDescriptor) {
			return ((MappedPropertyDescriptor) descriptor).getMappedPropertyType();
		} else {
			return descriptor.getPropertyType();
		}
	}

	/**
	 * <p>
	 * Return an accessible property getter method for this property, if there is one; otherwise
	 * return <code>null</code>.
	 * </p>
	 * 
	 * @param descriptor
	 *            Property descriptor to return a getter for
	 * @return The read method
	 */
	public Method getReadMethod(PropertyDescriptor descriptor) {
		return MethodUtils.getAccessibleMethod(descriptor.getReadMethod());
	}

	/**
	 * <p>
	 * Return an accessible property getter method for this property, if there is one; otherwise
	 * return <code>null</code>.
	 * </p>
	 * 
	 * @param clazz
	 *            The class of the read method will be invoked on
	 * @param descriptor
	 *            Property descriptor to return a getter for
	 * @return The read method
	 */
	Method getReadMethod(Class<?> clazz, PropertyDescriptor descriptor) {
		return MethodUtils.getAccessibleMethod(clazz, descriptor.getReadMethod());
	}

	/**
	 * Return the value of the specified simple property of the specified bean, with no type
	 * conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Name of the property to be extracted
	 * @return The property value
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception IllegalArgumentException
	 *                if the property name is nested or indexed
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public Object getSimpleProperty(Object bean, String name) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Validate the syntax of the property name
		if (resolver.hasNested(name)) {
			throw new IllegalArgumentException("Nested property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		} else if (resolver.isIndexed(name)) {
			throw new IllegalArgumentException("Indexed property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		} else if (resolver.isMapped(name)) {
			throw new IllegalArgumentException("Mapped property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		}

		// Retrieve the property getter method for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "' on class '"
					+ bean.getClass() + "'");
		}
		Method readMethod = getReadMethod(bean.getClass(), descriptor);
		if (readMethod == null) {
			throw new NoSuchMethodException("Property '" + name
					+ "' has no getter method in class '" + bean.getClass() + "'");
		}

		// Call the property getter and return the value
		Object value = invokeMethod(readMethod, bean, EMPTY_OBJECT_ARRAY);
		return value;
	}

	/**
	 * <p>
	 * Return an accessible property setter method for this property, if there is one; otherwise
	 * return <code>null</code>.
	 * </p>
	 * 
	 * @param descriptor
	 *            Property descriptor to return a setter for
	 * @return The write method
	 */
	public Method getWriteMethod(PropertyDescriptor descriptor) {
		return MethodUtils.getAccessibleMethod(descriptor.getWriteMethod());
	}

	/**
	 * <p>
	 * Return an accessible property setter method for this property, if there is one; otherwise
	 * return <code>null</code>.
	 * </p>
	 * 
	 * @param clazz
	 *            The class of the read method will be invoked on
	 * @param descriptor
	 *            Property descriptor to return a setter for
	 * @return The write method
	 */
	Method getWriteMethod(Class<?> clazz, PropertyDescriptor descriptor) {
		return MethodUtils.getAccessibleMethod(clazz, descriptor.getWriteMethod());
	}

	/**
	 * <p>
	 * Return <code>true</code> if the specified property name identifies a readable property on the
	 * specified bean; otherwise, return <code>false</code>.
	 * 
	 * @param bean
	 *            Bean to be examined
	 * @param name
	 *            Property name to be evaluated
	 * @return <code>true</code> if the property is readable, otherwise <code>false</code>
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is <code>null</code>
	 * 
	 * @since BeanUtils 1.6
	 */
	public boolean isReadable(Object bean, String name) {
		// Validate method parameters
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = null;
			try {
				nestedBean = getProperty(bean, next);
			} catch (IllegalAccessException e) {
				return false;
			} catch (InvocationTargetException e) {
				return false;
			} catch (NoSuchMethodException e) {
				return false;
			}
			if (nestedBean == null) {
				throw new NestedNullException("Null property value for '" + next
						+ "' on bean class '" + bean.getClass() + "'");
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		// Remove any subscript from the final name value
		name = resolver.getProperty(name);

		// Treat WrapDynaBean as special case - may be a write-only property
		// (see Jira issue# BEANUTILS-61)
		if (bean instanceof WrapDynaBean) {
			bean = ((WrapDynaBean) bean).getInstance();
		}

		// Return the requested result
		try {
			PropertyDescriptor desc = getPropertyDescriptor(bean, name);
			if (desc != null) {
				Method readMethod = getReadMethod(bean.getClass(), desc);
				if (readMethod == null) {
					if (desc instanceof IndexedPropertyDescriptor) {
						readMethod = ((IndexedPropertyDescriptor) desc).getIndexedReadMethod();
					} else if (desc instanceof MappedPropertyDescriptor) {
						readMethod = ((MappedPropertyDescriptor) desc).getMappedReadMethod();
					}
					readMethod = MethodUtils.getAccessibleMethod(bean.getClass(), readMethod);
				}
				return (readMethod != null);
			} else {
				return false;
			}
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * Return <code>true</code> if the specified property name identifies a writeable property on
	 * the specified bean; otherwise, return <code>false</code>.
	 * 
	 * @param bean
	 *            Bean to be examined (may be a {@link DynaBean}
	 * @param name
	 *            Property name to be evaluated
	 * @return <code>true</code> if the property is writeable, otherwise <code>false</code>
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is <code>null</code>
	 * 
	 * @since BeanUtils 1.6
	 */
	public boolean isWriteable(Object bean, String name) {
		// Validate method parameters
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = null;
			try {
				nestedBean = getProperty(bean, next);
			} catch (IllegalAccessException e) {
				return false;
			} catch (InvocationTargetException e) {
				return false;
			} catch (NoSuchMethodException e) {
				return false;
			}
			if (nestedBean == null) {
				throw new NestedNullException("Null property value for '" + next
						+ "' on bean class '" + bean.getClass() + "'");
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		// Remove any subscript from the final name value
		name = resolver.getProperty(name);

		// Return the requested result
		try {
			PropertyDescriptor desc = getPropertyDescriptor(bean, name);
			if (desc != null) {
				Method writeMethod = getWriteMethod(bean.getClass(), desc);
				if (writeMethod == null) {
					if (desc instanceof IndexedPropertyDescriptor) {
						writeMethod = ((IndexedPropertyDescriptor) desc).getIndexedWriteMethod();
					} else if (desc instanceof MappedPropertyDescriptor) {
						writeMethod = ((MappedPropertyDescriptor) desc).getMappedWriteMethod();
					}
					writeMethod = MethodUtils.getAccessibleMethod(bean.getClass(), writeMethod);
				}
				return (writeMethod != null);
			} else {
				return false;
			}
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	/**
	 * Set the value of the specified indexed property of the specified bean, with no type
	 * conversions. The zero-relative index of the required value must be included (in square
	 * brackets) as a suffix to the property name, or <code>IllegalArgumentException</code> will be
	 * thrown. In addition to supporting the JavaBeans specification, this method has been extended
	 * to support <code>List</code> objects as well.
	 * 
	 * @param bean
	 *            Bean whose property is to be modified
	 * @param name
	 *            <code>propertyname[index]</code> of the property value to be modified
	 * @param value
	 *            Value to which the specified property element should be set
	 * 
	 * @exception IndexOutOfBoundsException
	 *                if the specified index is outside the valid range for the underlying property
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public void setIndexedProperty(Object bean, String name, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Identify the index of the requested individual property
		int index = -1;
		try {
			index = resolver.getIndex(name);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid indexed property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}
		if (index < 0) {
			throw new IllegalArgumentException("Invalid indexed property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}

		// Isolate the name
		name = resolver.getProperty(name);

		// Set the specified indexed property value
		setIndexedProperty(bean, name, index, value);
	}

	/**
	 * Set the value of the specified indexed property of the specified bean, with no type
	 * conversions. In addition to supporting the JavaBeans specification, this method has been
	 * extended to support <code>List</code> objects as well.
	 * 
	 * @param bean
	 *            Bean whose property is to be set
	 * @param name
	 *            Simple property name of the property value to be set
	 * @param index
	 *            Index of the property value to be set
	 * @param value
	 *            Value to which the indexed property element is to be set
	 * 
	 * @exception IndexOutOfBoundsException
	 *                if the specified index is outside the valid range for the underlying property
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public void setIndexedProperty(Object bean, String name, int index, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null || name.length() == 0) {
			if (bean.getClass().isArray()) {
				Array.set(bean, index, value);
				return;
			} else if (bean instanceof List) {
				((List<Object>) bean).set(index, value);
				return;
			}
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Retrieve the property descriptor for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "' on bean class '"
					+ bean.getClass() + "'");
		}

		// Call the indexed setter method if there is one
		if (descriptor instanceof IndexedPropertyDescriptor) {
			Method writeMethod = ((IndexedPropertyDescriptor) descriptor).getIndexedWriteMethod();
			writeMethod = MethodUtils.getAccessibleMethod(bean.getClass(), writeMethod);
			if (writeMethod != null) {
				Object[] subscript = new Object[2];
				subscript[0] = new Integer(index);
				subscript[1] = value;
				try {
					if (log.isLoggable(Level.FINEST)) {
						String valueClassName = value == null ? "<null>" : value.getClass()
								.getName();
						log.log(Level.FINEST, "setSimpleProperty: Invoking method " + writeMethod
								+ " with index=" + index + ", value=" + value + " (class "
								+ valueClassName + ")");
					}
					invokeMethod(writeMethod, bean, subscript);
				} catch (InvocationTargetException e) {
					if (e.getTargetException() instanceof IndexOutOfBoundsException) {
						throw (IndexOutOfBoundsException) e.getTargetException();
					} else {
						throw e;
					}
				}
				return;
			}
		}

		// Otherwise, the underlying property must be an array or a list
		Method readMethod = getReadMethod(bean.getClass(), descriptor);
		if (readMethod == null) {
			throw new NoSuchMethodException("Property '" + name
					+ "' has no getter method on bean class '" + bean.getClass() + "'");
		}

		// Call the property getter to get the array or list
		Object array = invokeMethod(readMethod, bean, EMPTY_OBJECT_ARRAY);
		if (!array.getClass().isArray()) {
			if (array instanceof List) {
				// Modify the specified value in the List
				((List<Object>) array).set(index, value);
			} else {
				throw new IllegalArgumentException("Property '" + name
						+ "' is not indexed on bean class '" + bean.getClass() + "'");
			}
		} else {
			// Modify the specified value in the array
			Array.set(array, index, value);
		}
	}

	/**
	 * Set the value of the specified mapped property of the specified bean, with no type
	 * conversions. The key of the value to set must be included (in brackets) as a suffix to the
	 * property name, or <code>IllegalArgumentException</code> will be thrown.
	 * 
	 * @param bean
	 *            Bean whose property is to be set
	 * @param name
	 *            <code>propertyname(key)</code> of the property value to be set
	 * @param value
	 *            The property value to be set
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public void setMappedProperty(Object bean, String name, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Identify the key of the requested individual property
		String key = null;
		try {
			key = resolver.getKey(name);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid mapped property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}
		if (key == null) {
			throw new IllegalArgumentException("Invalid mapped property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}

		// Isolate the name
		name = resolver.getProperty(name);

		// Request the specified indexed property value
		setMappedProperty(bean, name, key, value);
	}

	/**
	 * Set the value of the specified mapped property of the specified bean, with no type
	 * conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be set
	 * @param name
	 *            Mapped property name of the property value to be set
	 * @param key
	 *            Key of the property value to be set
	 * @param value
	 *            The property value to be set
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public void setMappedProperty(Object bean, String name, String key, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}
		if (key == null) {
			throw new IllegalArgumentException("No key specified for property '" + name
					+ "' on bean class '" + bean.getClass() + "'");
		}

		// Retrieve the property descriptor for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "' on bean class '"
					+ bean.getClass() + "'");
		}

		if (descriptor instanceof MappedPropertyDescriptor) {
			// Call the keyed setter method if there is one
			Method mappedWriteMethod = ((MappedPropertyDescriptor) descriptor)
					.getMappedWriteMethod();
			mappedWriteMethod = MethodUtils.getAccessibleMethod(bean.getClass(), mappedWriteMethod);
			if (mappedWriteMethod != null) {
				Object[] params = new Object[2];
				params[0] = key;
				params[1] = value;
				if (log.isLoggable(Level.FINEST)) {
					String valueClassName = value == null ? "<null>" : value.getClass().getName();
					log.log(Level.FINEST, "setSimpleProperty: Invoking method " + mappedWriteMethod
							+ " with key=" + key + ", value=" + value + " (class " + valueClassName
							+ ")");
				}
				invokeMethod(mappedWriteMethod, bean, params);
			} else {
				throw new NoSuchMethodException("Property '" + name
						+ "' has no mapped setter method" + "on bean class '" + bean.getClass()
						+ "'");
			}
		} else {
			/* means that the result has to be retrieved from a map */
			Method readMethod = getReadMethod(bean.getClass(), descriptor);
			if (readMethod != null) {
				Object invokeResult = invokeMethod(readMethod, bean, EMPTY_OBJECT_ARRAY);
				/* test and fetch from the map */
				if (invokeResult instanceof java.util.Map) {
					((java.util.Map<String, Object>) invokeResult).put(key, value);
				}
			} else {
				throw new NoSuchMethodException("Property '" + name
						+ "' has no mapped getter method on bean class '" + bean.getClass() + "'");
			}
		}
	}

	/**
	 * Set the value of the (possibly nested) property of the specified name, for the specified
	 * bean, with no type conversions.
	 * <p>
	 * Example values for parameter "name" are:
	 * <ul>
	 * <li>"a" -- sets the value of property a of the specified bean</li>
	 * <li>"a.b" -- gets the value of property a of the specified bean, then on that object sets the
	 * value of property b.</li>
	 * <li>"a(key)" -- sets a value of mapped-property a on the specified bean. This effectively
	 * means bean.setA("key").</li>
	 * <li>"a[3]" -- sets a value of indexed-property a on the specified bean. This effectively
	 * means bean.setA(3).</li>
	 * </ul>
	 * 
	 * @param bean
	 *            Bean whose property is to be modified
	 * @param name
	 *            Possibly nested name of the property to be modified
	 * @param value
	 *            Value to which the property is to be set
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception IllegalArgumentException
	 *                if a nested reference to a property returns null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	@SuppressWarnings("unchecked")
	public void setNestedProperty(Object bean, String name, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Resolve nested references
		while (resolver.hasNested(name)) {
			String next = resolver.next(name);
			Object nestedBean = null;
			if (bean instanceof Map) {
				nestedBean = getPropertyOfMapBean((Map<String, Object>) bean, next);
			} else if (resolver.isMapped(next)) {
				nestedBean = getMappedProperty(bean, next);
			} else if (resolver.isIndexed(next)) {
				nestedBean = getIndexedProperty(bean, next);
			} else {
				nestedBean = getSimpleProperty(bean, next);
			}
			if (nestedBean == null) {
				// try to create a new instance of the Object!
				Class<?> nestedBeanType = getPropertyType(bean, next);
				try {
					nestedBean = nestedBeanType.newInstance();
					setProperty(bean, next, nestedBean);
				} catch (InstantiationException e) {
					throw new NestedNullException("Null property value for '" + name
							+ "' on bean class '" + bean.getClass()
							+ "'. Could not create a new instance!");
				}
			}
			bean = nestedBean;
			name = resolver.remove(name);
		}

		if (bean instanceof Map) {
			setPropertyOfMapBean((Map<String, Object>) bean, name, value);
		} else if (resolver.isMapped(name)) {
			setMappedProperty(bean, name, value);
		} else if (resolver.isIndexed(name)) {
			setIndexedProperty(bean, name, value);
		} else {
			setSimpleProperty(bean, name, value);
		}
	}

	/**
	 * This method is called by method setNestedProperty when the current bean is found to be a Map
	 * object, and defines how to deal with setting a property on a Map.
	 * <p>
	 * The standard implementation here is to:
	 * <ul>
	 * <li>call bean.set(propertyName) for all propertyName values.</li>
	 * <li>throw an IllegalArgumentException if the property specifier contains MAPPED_DELIM or
	 * INDEXED_DELIM, as Map entries are essentially simple properties; mapping and indexing
	 * operations do not make sense when accessing a map (even thought the returned object may be a
	 * Map or an Array).</li>
	 * </ul>
	 * <p>
	 * The default behaviour of beanutils 1.7.1 or later is for assigning to "a.b" to mean a.put(b,
	 * obj) always. However the behaviour of beanutils version 1.6.0, 1.6.1, 1.7.0 was for "a.b" to
	 * mean a.setB(obj) if such a method existed, and a.put(b, obj) otherwise. In version 1.5 it
	 * meant a.put(b, obj) always (ie the same as the behaviour in the current version). In versions
	 * prior to 1.5 it meant a.setB(obj) always. [yes, this is all <i>very</i> unfortunate]
	 * <p>
	 * Users who would like to customise the meaning of "a.b" in method setNestedProperty when a is
	 * a Map can create a custom subclass of this class and override this method to implement the
	 * behaviour of their choice, such as restoring the pre-1.4 behaviour of this class if they
	 * wish. When overriding this method, do not forget to deal with MAPPED_DELIM and INDEXED_DELIM
	 * characters in the propertyName.
	 * <p>
	 * Note, however, that the recommended solution for objects that implement Map but want their
	 * simple properties to come first is for <i>those</i> objects to override their get/put methods
	 * to implement that behaviour, and <i>not</i> to solve the problem by modifying the default
	 * behaviour of the PropertyUtilsBean class by overriding this method.
	 * 
	 * @param bean
	 *            Map bean
	 * @param propertyName
	 *            The property name
	 * @param value
	 *            the property value
	 * 
	 * @throws IllegalArgumentException
	 *             when the propertyName is regarded as being invalid.
	 * 
	 * @throws IllegalAccessException
	 *             just in case subclasses override this method to try to access real setter methods
	 *             and find permission is denied.
	 * 
	 * @throws InvocationTargetException
	 *             just in case subclasses override this method to try to access real setter
	 *             methods, and find it throws an exception when invoked.
	 * 
	 * @throws NoSuchMethodException
	 *             just in case subclasses override this method to try to access real setter
	 *             methods, and want to fail if no simple method is available.
	 * @since 1.8.0
	 */
	protected void setPropertyOfMapBean(Map<String, Object> bean, String propertyName, Object value)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (resolver.isMapped(propertyName)) {
			String name = resolver.getProperty(propertyName);
			if (name == null || name.length() == 0) {
				propertyName = resolver.getKey(propertyName);
			}
		}

		if (resolver.isIndexed(propertyName) || resolver.isMapped(propertyName)) {
			throw new IllegalArgumentException("Indexed or mapped properties are not supported on"
					+ " objects of type Map: " + propertyName);
		}

		bean.put(propertyName, value);
	}

	/**
	 * Set the value of the specified property of the specified bean, no matter which property
	 * reference format is used, with no type conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be modified
	 * @param name
	 *            Possibly indexed and/or nested name of the property to be modified
	 * @param value
	 *            Value to which this property is to be set
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public void setProperty(Object bean, String name, Object value) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		setNestedProperty(bean, name, value);
	}

	/**
	 * Set the value of the specified simple property of the specified bean, with no type
	 * conversions.
	 * 
	 * @param bean
	 *            Bean whose property is to be modified
	 * @param name
	 *            Name of the property to be modified
	 * @param value
	 *            Value to which the property should be set
	 * 
	 * @exception IllegalAccessException
	 *                if the caller does not have access to the property accessor method
	 * @exception IllegalArgumentException
	 *                if <code>bean</code> or <code>name</code> is null
	 * @exception IllegalArgumentException
	 *                if the property name is nested or indexed
	 * @exception InvocationTargetException
	 *                if the property accessor method throws an exception
	 * @exception NoSuchMethodException
	 *                if an accessor method for this propety cannot be found
	 */
	public void setSimpleProperty(Object bean, String name, Object value)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified");
		}
		if (name == null) {
			throw new IllegalArgumentException("No name specified for bean class '"
					+ bean.getClass() + "'");
		}

		// Validate the syntax of the property name
		if (resolver.hasNested(name)) {
			throw new IllegalArgumentException("Nested property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		} else if (resolver.isIndexed(name)) {
			throw new IllegalArgumentException("Indexed property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		} else if (resolver.isMapped(name)) {
			throw new IllegalArgumentException("Mapped property names are not allowed: Property '"
					+ name + "' on bean class '" + bean.getClass() + "'");
		}

		// Retrieve the property setter method for the specified property
		PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
		if (descriptor == null) {
			throw new NoSuchMethodException("Unknown property '" + name + "' on class '"
					+ bean.getClass() + "'");
		}
		Method writeMethod = getWriteMethod(bean.getClass(), descriptor);
		if (writeMethod == null) {
			throw new NoSuchMethodException("Property '" + name
					+ "' has no setter method in class '" + bean.getClass() + "'");
		}

		// Call the property setter method
		Object[] values = new Object[1];
		if (descriptor.getPropertyType() != String.class && value instanceof String) {
			try {
				values[0] = convertPropertyStringToObject(descriptor.getPropertyType(),
						String.valueOf(value));
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			values[0] = value;
		}
		if (log.isLoggable(Level.FINEST)) {
			String valueClassName = value == null ? "<null>" : value.getClass().getName();
			log.log(Level.FINEST, "setSimpleProperty: Invoking method " + writeMethod
					+ " with value " + value + " (class " + valueClassName + ")");
		}
		invokeMethod(writeMethod, bean, values);
	}

	/** This just catches and wraps IllegalArgumentException. */
	private Object invokeMethod(Method method, Object bean, Object[] values)
			throws IllegalAccessException, InvocationTargetException {
		if (bean == null) {
			throw new IllegalArgumentException("No bean specified "
					+ "- this should have been checked before reaching this method");
		}

		try {
			return method.invoke(bean, values);
		} catch (NullPointerException cause) {
			// JDK 1.3 and JDK 1.4 throw NullPointerException if an argument is
			// null for a primitive value (JDK 1.5+ throw IllegalArgumentException)
			String valueString = "";
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					if (i > 0) {
						valueString += ", ";
					}
					if (values[i] == null) {
						valueString += "<null>";
					} else {
						valueString += (values[i]).getClass().getName();
					}
				}
			}
			String expectedString = "";
			Class<?>[] parTypes = method.getParameterTypes();
			if (parTypes != null) {
				for (int i = 0; i < parTypes.length; i++) {
					if (i > 0) {
						expectedString += ", ";
					}
					expectedString += parTypes[i].getName();
				}
			}
			IllegalArgumentException e = new IllegalArgumentException("Cannot invoke "
					+ method.getDeclaringClass().getName() + "." + method.getName()
					+ " on bean class '" + bean.getClass() + "' - " + cause.getMessage()
					// as per https://issues.apache.org/jira/browse/BEANUTILS-224
					+ " - had objects of type \"" + valueString + "\" but expected signature \""
					+ expectedString + "\"");
			if (!BeanUtils.initCause(e, cause)) {
				log.log(Level.SEVERE, "Method invocation failed", cause);
			}
			throw e;
		} catch (IllegalArgumentException cause) {
			String valueString = "";
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					if (i > 0) {
						valueString += ", ";
					}
					if (values[i] == null) {
						valueString += "<null>";
					} else {
						valueString += (values[i]).getClass().getName();
					}
				}
			}
			String expectedString = "";
			Class<?>[] parTypes = method.getParameterTypes();
			if (parTypes != null) {
				for (int i = 0; i < parTypes.length; i++) {
					if (i > 0) {
						expectedString += ", ";
					}
					expectedString += parTypes[i].getName();
				}
			}
			IllegalArgumentException e = new IllegalArgumentException("Cannot invoke "
					+ method.getDeclaringClass().getName() + "." + method.getName()
					+ " on bean class '" + bean.getClass() + "' - " + cause.getMessage()
					// as per https://issues.apache.org/jira/browse/BEANUTILS-224
					+ " - had objects of type \"" + valueString + "\" but expected signature \""
					+ expectedString + "\"");
			if (!BeanUtils.initCause(e, cause)) {
				log.log(Level.SEVERE, "Method invocation failed", cause);
			}
			throw e;
		}
	}

	/**
	 * Convert the given (property-)value (a string) to the given type.
	 * 
	 * @param clazz
	 *            the type of the property (to convert the property to)
	 * @param value
	 *            the property-value (as string)
	 * @return the property-value (converted to the expected type)
	 * @throws ParseException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	static Object convertPropertyStringToObject(Class<?> clazz, String value)
			throws ParseException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (value == null) {
			return null;
		}
		try {
			if (clazz == Integer.class || clazz == int.class) {
				Double d = Double.parseDouble(value);
				if (d != null) {
					return d.intValue();
				} else {
					return null;
				}
			} else if (clazz == Double.class || clazz == double.class) {
				return Double.parseDouble(value);
			} else if (clazz == Long.class || clazz == long.class) {
				Double d = Double.parseDouble(value);
				if (d != null) {
					return d.longValue();
				} else {
					return null;
				}
			} else if (clazz == Float.class || clazz == float.class) {
				return Float.parseFloat(value);
			} else if (clazz == Boolean.class || clazz == boolean.class) {
				return Boolean.parseBoolean(value);
			} else if (clazz == BigDecimal.class) {
				return new BigDecimal(value);
			} else if (clazz == Date.class) {
				return SimpleDateFormat.getDateInstance().parse(value);
			} else if (clazz.isEnum()) {
				return clazz.getMethod("valueOf", String.class).invoke(clazz, value);
			}
			return value; // value of
		} catch (NumberFormatException | ParseException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz,
					value });
			throw e;
		} catch (IllegalArgumentException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz,
					value });
			throw e;
		} catch (SecurityException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			log.log(Level.WARNING, "Can't convert String to Obj - {0} - {1}", new Object[] { clazz,
					value });
			throw e;
		}
	}
}
