package com.jexunit.core.data;

import ognl.OgnlException;

/**
 * @author fabian
 *
 */
public class NoSuchCollectionElementException extends OgnlException {

	private static final long serialVersionUID = 1L;

	private String targetCollection;
	private String collectionExpression;

	public NoSuchCollectionElementException(String target, String name) {
		super(getReason(target, name));
		this.targetCollection = target;
		this.collectionExpression = name;
	}

	static String getReason(String target, String name) {
		String ret = null;

		if (target == null) {
			ret = "null";
		} else {
			ret = target;
		}

		ret += "[" + name + "]";

		return ret;
	}

	/**
	 * @return the targetCollection
	 */
	public String getTargetCollection() {
		return targetCollection;
	}

	/**
	 * @return the collectionExpression
	 */
	public String getCollectionExpression() {
		return collectionExpression;
	}

}
