/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import org.nocturne.exception.ReflectionException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Reflection utilities.
 *
 * @author Mike Mirzayanov
 */
public class ReflectionUtil {
    private static final Map<Class<?>, Class<?>> originalClassByWrapperClass = new HashMap<>();
    private static final ReadWriteLock originalClassByWrapperClassMapLock = new ReentrantReadWriteLock();

    /**
     * Invokes method by name for object, finds method among methods
     * of specified class.
     *
     * @param clazz      Class instance where to find method.
     * @param object     Object which method will be invoked.
     * @param methodName Method name.
     * @param args       Method arguments.
     * @return Object Method return value.
     * @throws org.nocturne.exception.ReflectionException
     *          If it can't invoke method.
     */
    public static Object invoke(Class<?> clazz, Object object, String methodName, Object... args) throws ReflectionException {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == args.length) {
                try {
                    method.setAccessible(true);
                    return method.invoke(object, args);
                } catch (Exception e) {
                    throw new ReflectionException(
                            "Can't invoke method " + methodName + " of the class " + clazz.getName() + '.', e
                    );
                }
            }
        }

        if (clazz.getSuperclass() == null) {
            throw new ReflectionException("Can't find method " + methodName + " of the class " + clazz.getName() + '.');
        } else {
            return invoke(clazz.getSuperclass(), object, methodName, args);
        }
    }

    /**
     * Invokes method by name.
     *
     * @param object     Object which method will be invoked.
     * @param methodName Method name.
     * @param args       Method arguments.
     * @return Object Method return value.
     * @throws org.nocturne.exception.ReflectionException
     *          If it can't invoke method.
     */
    public static Object invoke(Object object, String methodName, Object... args) throws ReflectionException {
        Class<?> clazz = object.getClass();
        return invoke(clazz, object, methodName, args);
    }

    /**
     * Original class can be wrapped by Google Guice because of IoC.
     * The method returns original class by possible wrapped.
     *
     * @param wrapperClass wrapper class or original class.
     * @return original class.
     */
    public static Class<?> getOriginalClass(Class<?> wrapperClass) {
        Class<?> originalClass;

        Lock readLock = originalClassByWrapperClassMapLock.readLock();
        readLock.lock();
        try {
            originalClass = originalClassByWrapperClass.get(wrapperClass);
            if (originalClass != null) {
                return originalClass;
            }
        } finally {
            readLock.unlock();
        }

        originalClass = wrapperClass;
        while (isWrapperClass(originalClass)) {
            originalClass = originalClass.getSuperclass();
        }

        Lock writeLock = originalClassByWrapperClassMapLock.writeLock();
        writeLock.lock();
        try {
            originalClassByWrapperClass.put(wrapperClass, originalClass);
            return originalClass;
        } finally {
            writeLock.unlock();
        }
    }

    private static boolean isWrapperClass(Class<?> clazz) {
        String className = clazz.getName();
        return className.contains("$$") || className.contains("EnhancerByGuice");
    }
}
