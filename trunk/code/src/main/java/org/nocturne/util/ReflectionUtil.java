/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import org.nocturne.exception.ReflectionException;
import org.nocturne.main.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reflection utilities.
 *
 * @author Mike Mirzayanov
 */
public class ReflectionUtil {
    private static final ConcurrentMap<Class<?>, Class<?>> realClassByGeneratedClass = new ConcurrentHashMap<>();

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
     * @param clazz Component class.
     * @return Original class can be wrapped by Google Guice because of IoC.
     *         The method returns original class by possible wrapped.
     */
    @SuppressWarnings("ConstantConditions")
    public static Class<?> getRealComponentClass(Class<? extends Component> clazz) {
        Class<?> result = realClassByGeneratedClass.get(clazz);
        if (result != null) {
            return result;
        }

        result = clazz;
        while (isGeneratedClass(result)) {
            result = result.getSuperclass();
        }

        realClassByGeneratedClass.putIfAbsent(clazz, result);
        return realClassByGeneratedClass.get(clazz);
    }

    private static boolean isGeneratedClass(Class<?> clazz) {
        return clazz.getName().contains("$$") || clazz.getName().contains("EnhancerByGuice");
    }
}
