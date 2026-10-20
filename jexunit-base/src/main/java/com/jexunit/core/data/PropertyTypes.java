package com.jexunit.core.data;

import ognl.ASTChain;
import ognl.ASTProperty;
import ognl.Node;
import ognl.OgnlRuntime;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * Helper for resolving the (generic) types of the properties along an OGNL property expression, starting at the type
 * of the root object. This way the element type of a list (<code>items[0]</code>) is known even if the list is
 * declared in a superclass or nested in a sub-entity (<code>customer.addresses[0]</code>).
 *
 * @author fabian
 */
final class PropertyTypes {

    private PropertyTypes() {
    }

    /**
     * Resolve the type of the value the first <code>count</code> nodes of the given expression lead to, starting at
     * the given root type.
     *
     * @param rootType   the type of the root object
     * @param expression the (parsed) expression: a chain or a single property
     * @param count      the number of nodes of the chain to follow
     * @return the resolved type or <code>null</code>, if it cannot be resolved
     */
    static Type resolve(final Class<?> rootType, final Node expression, final int count) {
        Type type = rootType;
        for (int i = 0; i < count && type != null; i++) {
            final Node node = expression instanceof ASTChain ? expression.jjtGetChild(i) : expression;
            if (!(node instanceof ASTProperty)) {
                return null;
            }
            type = ((ASTProperty) node).isIndexedAccess() ? elementType(type) : propertyType(type, node.toString());
        }
        return type;
    }

    /**
     * Get the number of nodes of the given expression (the number of chain elements, or 1 for a single property).
     *
     * @param expression the (parsed) expression
     * @return the number of nodes
     */
    static int length(final Node expression) {
        return expression instanceof ASTChain ? expression.jjtGetNumChildren() : 1;
    }

    /**
     * Resolve the type of the element of the collection, that is accessed by the given index node. The index node is
     * the (indexed) property node of a chain, or a node inside of it.
     *
     * @param rootType the type of the root object
     * @param node     the index node (or a node inside of it)
     * @return the element type or <code>null</code>, if it cannot be resolved
     */
    static Class<?> resolveElementType(final Class<?> rootType, final Node node) {
        final Node indexNode = getChainElement(node);
        if (indexNode == null) {
            return null;
        }
        final int position = getPosition(indexNode);
        return position < 0 ? null : rawClass(elementType(resolve(rootType, indexNode.jjtGetParent(), position)));
    }

    /**
     * Get the name of the collection property, that is accessed by the given index node (like <code>items</code> for
     * <code>order.items[0]</code>).
     *
     * @param node the index node (or a node inside of it)
     * @return the name of the collection property or <code>null</code>, if there is no property before the index
     */
    static String getCollectionPropertyName(final Node node) {
        final Node indexNode = getChainElement(node);
        final int position = indexNode == null ? -1 : getPosition(indexNode);
        return position > 0 ? indexNode.jjtGetParent().jjtGetChild(position - 1).toString() : null;
    }

    /**
     * Get the element of the chain, the given node belongs to (the node itself or one of its parents).
     */
    private static Node getChainElement(final Node node) {
        Node chainElement = node;
        while (chainElement != null && !(chainElement.jjtGetParent() instanceof ASTChain)) {
            chainElement = chainElement.jjtGetParent();
        }
        return chainElement;
    }

    /**
     * Get the position of the given element in its chain.
     */
    private static int getPosition(final Node chainElement) {
        final Node chain = chainElement.jjtGetParent();
        for (int i = 0; i < chain.jjtGetNumChildren(); i++) {
            if (chain.jjtGetChild(i) == chainElement) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the type of the property with the given name of the given type (for a map: the type of the values).
     *
     * @param ownerType the type declaring the property
     * @param name      the name of the property
     * @return the type of the property or <code>null</code>, if it cannot be resolved
     */
    static Type propertyType(final Type ownerType, final String name) {
        final Class<?> ownerClass = rawClass(ownerType);
        if (ownerClass == null) {
            return null;
        }
        if (Map.class.isAssignableFrom(ownerClass)) {
            return typeArgument(ownerType, 1);
        }
        try {
            final PropertyDescriptor pd = OgnlRuntime.getPropertyDescriptor(ownerClass, name);
            if (pd != null && pd.getReadMethod() != null) {
                return pd.getReadMethod().getGenericReturnType();
            }
        } catch (final Exception e) {
            // try the field
        }
        for (Class<?> c = ownerClass; c != null; c = c.getSuperclass()) {
            for (final Field field : c.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return field.getGenericType();
                }
            }
        }
        return null;
    }

    /**
     * Get the type of the elements of the given collection/array type (for a map: the type of the values).
     *
     * @param type the collection, array or map type
     * @return the element type or <code>null</code>, if it cannot be resolved
     */
    static Type elementType(final Type type) {
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        }
        final Class<?> rawClass = rawClass(type);
        if (rawClass == null) {
            return null;
        }
        if (rawClass.isArray()) {
            return rawClass.getComponentType();
        }
        if (Collection.class.isAssignableFrom(rawClass)) {
            return typeArgument(type, 0);
        }
        if (Map.class.isAssignableFrom(rawClass)) {
            return typeArgument(type, 1);
        }
        return null;
    }

    /**
     * Get the raw class of the given type.
     *
     * @param type the type
     * @return the raw class or <code>null</code> for type variables, wildcards, ... (or a <code>null</code> type)
     */
    static Class<?> rawClass(final Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof GenericArrayType) {
            final Class<?> component = rawClass(((GenericArrayType) type).getGenericComponentType());
            return component == null ? null : Array.newInstance(component, 0).getClass();
        }
        return null;
    }

    private static Type typeArgument(final Type type, final int index) {
        if (type instanceof ParameterizedType) {
            final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            return arguments.length > index ? arguments[index] : null;
        }
        return null;
    }

}
