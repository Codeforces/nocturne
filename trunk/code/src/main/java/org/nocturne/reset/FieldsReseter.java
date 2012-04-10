package org.nocturne.reset;

import org.nocturne.exception.ConfigurationException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.Component;
import org.nocturne.main.Frame;
import org.nocturne.main.Page;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mike Mirzayanov
 */
public class FieldsReseter {
    private static final Map<Class<?>, Object> PRIMITIVES_DEFAULT_VALUES
            = new ConcurrentHashMap<Class<?>, Object>();

    private static final ConcurrentMap<AnnotatedElement, Boolean> RESET_ANNOTATIONS_CACHE
            = new ConcurrentHashMap<AnnotatedElement, Boolean>();

    private static final ConcurrentMap<AnnotatedElement, Boolean> PERSIST_ANNOTATIONS_CACHE
            = new ConcurrentHashMap<AnnotatedElement, Boolean>();

    private final Component component;
    private final List<Field> fieldsToReset = new ArrayList<Field>();
    private final ResetStrategy componentStrategy;

    private static boolean hasResetAnnotation(AnnotatedElement annotatedElement) {
        Boolean result = RESET_ANNOTATIONS_CACHE.get(annotatedElement);
        if (result != null) {
            return result;
        }

        result = false;
        Annotation[] annotations = annotatedElement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (ApplicationContext.getInstance().getResetAnnotations().contains(annotation.annotationType().getName())) {
                result = true;
                break;
            }
        }

        RESET_ANNOTATIONS_CACHE.putIfAbsent(annotatedElement, result);
        return result;
    }

    private static boolean hasPersistAnnotation(AnnotatedElement annotatedElement) {
        Boolean result = PERSIST_ANNOTATIONS_CACHE.get(annotatedElement);
        if (result != null) {
            return result;
        }

        result = false;
        Annotation[] annotations = annotatedElement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (ApplicationContext.getInstance().getPersistAnnotations().contains(annotation.annotationType().getName())) {
                result = true;
                break;
            }
        }

        PERSIST_ANNOTATIONS_CACHE.putIfAbsent(annotatedElement, result);
        return result;
    }

    public FieldsReseter(Component component) {
        componentStrategy = getStrategy(ApplicationContext.getInstance().getResetStrategy(),
                hasResetAnnotation(component.getClass()),
                hasPersistAnnotation(component.getClass()),
                component.getClass().getCanonicalName());

        this.component = component;
        addFieldsToReset();
    }

    private void addFieldsToReset() {
        Class<?> clazz = component.getClass();
        while (!isNocturneComponent(clazz)) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (Modifier.isStatic(declaredField.getModifiers())
                        || Modifier.isFinal(declaredField.getModifiers())) {
                    continue;
                }

                ResetStrategy fieldStrategy = getStrategy(
                        componentStrategy,
                        hasResetAnnotation(declaredField), hasPersistAnnotation(declaredField),
                        declaredField.toString()
                );

                if (fieldStrategy == ResetStrategy.RESET) {
                    fieldsToReset.add(declaredField);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static boolean isNocturneComponent(Class<?> clazz) {
        return clazz.getCanonicalName().equals(Component.class.getCanonicalName())
                || clazz.getCanonicalName().equals(Page.class.getCanonicalName())
                || clazz.getCanonicalName().equals(Frame.class.getCanonicalName());
    }

    public void resetFields() {
        for (Field field : fieldsToReset) {
            resetField(field);
        }
    }

    private void resetField(Field field) {
        boolean accessible = field.isAccessible();
        try {
            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                resetPrimitiveField(field);
                return;
            }

            try {
                field.set(component, null);
            } catch (IllegalAccessException ignored) {
                // No operations.
            }
        } finally {
            field.setAccessible(accessible);
        }
    }

    private void resetPrimitiveField(Field field) {
        Class<?> type = field.getType();
        if (type != void.class) {
            try {
                field.set(component, PRIMITIVES_DEFAULT_VALUES.get(type));
            } catch (IllegalAccessException ignored) {
                // No operations.
            }
        }
    }

    private static ResetStrategy getStrategy(
            ResetStrategy defaultStrategy, boolean hasReset, boolean hasPersist, String name) {
        if (hasPersist && hasReset) {
            throw new ConfigurationException("It is impossible to use " +
                    "Reset and Persist at the same time [name=" + name + "].");
        }

        if (hasReset) {
            return ResetStrategy.RESET;
        }

        if (hasPersist) {
            return ResetStrategy.PERSIST;
        }

        return defaultStrategy;
    }

    static {
        PRIMITIVES_DEFAULT_VALUES.put(int.class, 0);
        PRIMITIVES_DEFAULT_VALUES.put(long.class, 0L);
        PRIMITIVES_DEFAULT_VALUES.put(double.class, 0.0D);
        PRIMITIVES_DEFAULT_VALUES.put(float.class, 0.0F);
        PRIMITIVES_DEFAULT_VALUES.put(byte.class, (byte) 0);
        PRIMITIVES_DEFAULT_VALUES.put(short.class, (short) 0);
        PRIMITIVES_DEFAULT_VALUES.put(boolean.class, false);
    }
}
