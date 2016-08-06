package com.jexunit.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Created by Fabian on 30.07.2016.
 */
public class AuditProxy implements InvocationHandler {

	private static final Logger logger = Logger.getAnonymousLogger();
	private Object obj;

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(T obj) {
		return (T) Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(),
				new AuditProxy(obj));
	}

	public AuditProxy(Object obj) {
		this.obj = obj;
	}

	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
		Object result;
		try {
			logger.info("before method " + m.getName());
			long start = System.nanoTime();
			result = m.invoke(obj, args);
			long end = System.nanoTime();
			logger.info(String.format("%s took %d ns", m.getName(), (end - start)));
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		} catch (Exception e) {
			throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
		} finally {
			logger.info("after method " + m.getName());
		}
		return result;
	}
}