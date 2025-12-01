/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.nocturne.annotation.Action;
import org.nocturne.annotation.Invalid;
import org.nocturne.annotation.Parameter;
import org.nocturne.annotation.Validate;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.util.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores information about magic methods in the component.
 *
 * @author Mike Mirzayanov
 */
class ActionMap {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ActionMap.class);

    /* Default action has empty key "". */
    private final Map<String, ActionMethod> actions = new ConcurrentHashMap<>();

    /* Default validator has empty key "". */
    private final Map<String, FastMethod> validators = new ConcurrentHashMap<>();

    /* Default invalid method has empty key "". */
    private final Map<String, FastMethod> invalids = new ConcurrentHashMap<>();

    ActionMap(Class<? extends Component> pageClass) {
        FastClass clazz = FastClass.create(pageClass);

        List<Method> methods = new ArrayList<>();
        Class<?> auxClass = pageClass;
        while (auxClass != null) {
            methods.addAll(Arrays.asList(auxClass.getDeclaredMethods()));
            auxClass = auxClass.getSuperclass();
        }

        for (Method method : methods) {
            processMethod(clazz, method);
        }

        for (Method method : methods) {
            processMethodAsDefault(clazz, method);
        }
    }

    private void processMethodAsDefault(FastClass clazz, Method method) {
        if (!actions.containsKey("") && "action".equals(method.getName()) && method.getParameterTypes().length == 0) {
            if (method.getReturnType() != void.class) {
                logger.error("Default action method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
                throw new ConfigurationException("Default action method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            actions.put("", new ActionMethod(clazz.getMethod(method), method.getAnnotation(Action.class)));
        }

        if (!validators.containsKey("") && "validate".equals(method.getName()) && method.getParameterTypes().length == 0) {
            if (method.getReturnType() != boolean.class) {
                logger.error("Default validation method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
                throw new ConfigurationException("Default validation method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
            }
            validators.put("", clazz.getMethod(method));
        }

        if (!invalids.containsKey("") && "invalid".equals(method.getName()) && method.getParameterTypes().length == 0) {
            if (method.getReturnType() != void.class) {
                logger.error("Default invalid method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
                throw new ConfigurationException("Default invalid method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            invalids.put("", clazz.getMethod(method));
        }
    }

    private static void ensureProperlyAnnotatedParameters(Method method) {
        if (method.getParameterTypes().length != method.getParameterAnnotations().length) {
            logger.error("Expected \"method.getParameterTypes().length != method.getParameterAnnotations().length\".");
            throw new NocturneException("Expected \"method.getParameterTypes().length != method.getParameterAnnotations().length\".");
        }

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (Annotation[] annotations : parameterAnnotations) {
            boolean hasParameter = false;
            boolean hasNamedParameter = false;
            for (Annotation annotation : annotations) {
                if (annotation instanceof Parameter) {
                    hasParameter = true;
                    hasNamedParameter = !StringUtil.isEmpty(((Parameter) annotation).name());
                }
            }
            if (!hasParameter) {
                logger.error("Each parameter of the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should be annotated with @Parameter.");
                throw new ConfigurationException("Each parameter of the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should be annotated with @Parameter.");
            }
            if (!hasNamedParameter) {
                logger.error("Each @Parameter in the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should have name.");
                throw new ConfigurationException("Each @Parameter in the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should have name.");
            }
        }
    }

    private void processMethod(FastClass clazz, Method method) {
        Action action = method.getAnnotation(Action.class);

        if (action != null) {
            if (actions.containsKey(action.value())) {
                logger.error("There are two or more methods for " +
                        clazz.getName() + " marked with @Action[" + action.value() + "].");
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Action[" + action.value() + "].");
            }

            ensureProperlyAnnotatedParameters(method);

            if (method.getReturnType() != void.class) {
                logger.error("Method with annotation @Action [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
                throw new ConfigurationException("Method with annotation @Action [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }

            actions.put(action.value(), new ActionMethod(clazz.getMethod(method), action));
        }

        Validate validate = method.getAnnotation(Validate.class);

        if (validate != null) {
            if (validators.containsKey(validate.value())) {
                logger.error("There are two or more methods for " +
                        clazz.getName() + " marked with @Validate[" + validate.value() + "].");
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Validate[" + validate.value() + "].");
            }

            ensureProperlyAnnotatedParameters(method);

            if (method.getReturnType() != boolean.class) {
                logger.error("Method with annotation @Validate [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
                throw new ConfigurationException("Method with annotation @Validate [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
            }

            validators.put(validate.value(), clazz.getMethod(method));
        }

        Invalid invalid = method.getAnnotation(Invalid.class);

        if (invalid != null) {
            if (invalids.containsKey(invalid.value())) {
                logger.error("There are two or more methods for " +
                        clazz.getName() + " marked with @Invalid[" + invalid.value() + "].");
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Invalid[" + invalid.value() + "].");
            }

            ensureProperlyAnnotatedParameters(method);

            if (method.getReturnType() != void.class) {
                logger.error("Method with annotation @Invalid [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
                throw new ConfigurationException("Method with annotation @Invalid [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }

            invalids.put(invalid.value(), clazz.getMethod(method));
        }
    }

    ActionMethod getActionMethod(String action) {
        if (actions.containsKey(action)) {
            return actions.get(action);
        } else {
            return actions.get("");
        }
    }

    FastMethod getValidateMethod(String action) {
        if (validators.containsKey(action)) {
            return validators.get(action);
        } else {
            return validators.get("");
        }
    }

    FastMethod getInvalidMethod(String action) {
        if (invalids.containsKey(action)) {
            return invalids.get(action);
        } else {
            return invalids.get("");
        }
    }

    public static final class ActionMethod {
        private final FastMethod method;
        private final Action action;

        private ActionMethod(FastMethod method, Action action) {
            this.method = method;
            this.action = action;
        }

        public FastMethod getMethod() {
            return method;
        }

        public Action getAction() {
            return action;
        }
    }
}
