/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.nocturne.annotation.Action;
import org.nocturne.annotation.Invalid;
import org.nocturne.annotation.Validate;
import org.nocturne.exception.ConfigurationException;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Stores information about magic methods in the component.
 *
 * @author Mike Mirzayanov
 */
class ActionMap {
    /* Default action has empty key "". */
    private final Map<String, FastMethod> actions = new Hashtable<String, FastMethod>();

    /* Default validator has empty key "". */
    private final Map<String, FastMethod> validators = new Hashtable<String, FastMethod>();

    /* Default invalid method has empty key "". */
    private final Map<String, FastMethod> invalids = new Hashtable<String, FastMethod>();

    ActionMap(Class<? extends Component> pageClass) {
        FastClass clazz = FastClass.create(pageClass);

        List<Method> methods = new ArrayList<Method>();
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
                throw new ConfigurationException("Default action method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            actions.put("", clazz.getMethod(method));
        }

        if (!validators.containsKey("") && "validate".equals(method.getName()) && method.getParameterTypes().length == 0) {
            if (method.getReturnType() != boolean.class) {
                throw new ConfigurationException("Default validation method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
            }
            validators.put("", clazz.getMethod(method));
        }

        if (!invalids.containsKey("") && "invalid".equals(method.getName()) && method.getParameterTypes().length == 0) {
            if (method.getReturnType() != void.class) {
                throw new ConfigurationException("Default invalid method [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            invalids.put("", clazz.getMethod(method));
        }
    }

    private boolean hasNoParams(Method method) {
        return method.getParameterTypes().length == 0;
    }

    private void processMethod(FastClass clazz, Method method) {
        Action action = method.getAnnotation(Action.class);

        if (action != null) {
            if (actions.containsKey(action.value())) {
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Action[" + action.value() + "].");
            }
            if (!hasNoParams(method)) {
                throw new ConfigurationException("Method with annotation @Action [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] shouldn't have any params.");
            }
            if (method.getReturnType() != void.class) {
                throw new ConfigurationException("Method with annotation @Action [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            actions.put(action.value(), clazz.getMethod(method));
        }

        Validate validate = method.getAnnotation(Validate.class);

        if (validate != null) {
            if (validators.containsKey(validate.value())) {
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Validate[" + validate.value() + "].");
            }
            if (!hasNoParams(method)) {
                throw new ConfigurationException("Method with annotation @Validate [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] shouldn't have any params.");
            }
            if (method.getReturnType() != boolean.class) {
                throw new ConfigurationException("Method with annotation @Validate [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return boolean.");
            }
            validators.put(validate.value(), clazz.getMethod(method));
        }

        Invalid invalid = method.getAnnotation(Invalid.class);

        if (invalid != null) {
            if (invalids.containsKey(invalid.value())) {
                throw new ConfigurationException("There are two or more methods for " +
                        clazz.getName() + " marked with @Invalid[" + invalid.value() + "].");
            }
            if (!hasNoParams(method)) {
                throw new ConfigurationException("Method with annotation @Invalid [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] shouldn't have any params.");
            }
            if (method.getReturnType() != void.class) {
                throw new ConfigurationException("Method with annotation @Invalid [name=" + method.getName() + ", " +
                        "class=" + clazz.getName() + "] should return void.");
            }
            invalids.put(invalid.value(), clazz.getMethod(method));
        }
    }

    FastMethod getActionMethod(String action) {
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
}
