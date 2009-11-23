/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import org.nocturne.exception.ConfigurationException;
import org.nocturne.annotation.Parameter;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Each component has an private instance of ParametersInjector.
 * It will process @Parameter annotations.
 * <p/>
 * Also this class can be used if you want to
 * inject parameters into some object.
 *
 * @author Mike Mirzayanov
 */
public class ParametersInjector {
    /** Injection target object. */
    private Object component;

    /** Stores information about  */
    private Set<InjectField> fields;

    /** @param component Object which has fields with @Parameter annotation. */
    public ParametersInjector(Object component) {
        this.component = component;
    }

    /** @param request Request to be analyzed to find parameters for injection. */
    public void inject(HttpServletRequest request) {
        if (fields == null) {
            scanFields();
        }

        for (InjectField field : fields) {
            String key = field.parameter.name().isEmpty()
                    ? field.field.getName() : field.parameter.name();

            String value = request.getParameter(key);
            if (value == null) {
                Object attr = request.getAttribute(ApplicationContext.getInstance().getAdditionalParamsRequestAttributePrefix() + key);
                if (attr != null) {
                    value = attr.toString();
                }
            }

            setupField(field, value);
        }
    }

    private void setupField(InjectField field, String value) {
        if (value == null) {
            setupFieldFromNull(field);
        } else {
            value = field.parameter.stripMode().strip(value);
            setupFieldFromPreparedAndNotNullValue(field, value);
        }
    }

    @SuppressWarnings({"ConstantConditions"})
    private void setupFieldFromPreparedAndNotNullValue(InjectField field, String value) {
        Class<?> clazz = field.field.getType();

        Object assign = null;
        boolean processed = false;

        if (clazz.equals(String.class)) {
            processed = true;
            assign = value;
        }

        if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            processed = true;
            try {
                assign = Boolean.valueOf(value);
            } catch (Exception e) {
                assign = false;
            }
        }

        if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            processed = true;
            try {
                assign = Integer.valueOf(value);
            } catch (Exception e) {
                assign = 0;
            }
        }

        if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            processed = true;
            try {
                assign = Long.valueOf(value);
            } catch (Exception e) {
                assign = 0L;
            }
        }

        if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            processed = true;
            try {
                assign = Double.valueOf(value);
            } catch (Exception e) {
                assign = 0.0;
            }
        }

        if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            processed = true;
            try {
                assign = Float.valueOf(value);
            } catch (Exception e) {
                assign = 0.0F;
            }
        }

        if (clazz.isEnum()) {
            processed = true;
            Object[] consts = clazz.getEnumConstants();
            for (Object constName : consts) {
                if (constName.toString().equalsIgnoreCase(value)) {
                    assign = constName;
                }
            }
        }

        if (!processed) {
            throw new ConfigurationException("Field " + field.field.getName() + " of "
                    + field.field.getDeclaringClass().getName() + " has unexpected type " + clazz.getName() + ".");
        } else {
            setFieldValue(field, assign);
        }
    }

    private void setupFieldFromNull(InjectField field) {
        Class<?> clazz = field.field.getType();

        Object assign = null;

        if (clazz.equals(String.class) || clazz.isEnum()) {
            assign = null;
        }

        if (clazz.equals(Boolean.class) || clazz.equals(Integer.class) 
                || clazz.equals(Long.class) || clazz.equals(Double.class) 
                || clazz.equals(Float.class)) {
            assign = null;
        }

        if (clazz.equals(boolean.class)) {
            assign = false;
        }

        if (clazz.equals(int.class)) {
            assign = Integer.valueOf(0);
        }

        if (clazz.equals(long.class)) {
            assign = Long.valueOf(0L);
        }

        if (clazz.equals(double.class)) {
            assign = Double.valueOf(0.0);
        }

        if (clazz.equals(float.class)) {
            assign = Float.valueOf(0.0F);
        }

        setFieldValue(field, assign);
    }

    private void setFieldValue(InjectField field, Object assign) {
        field.field.setAccessible(true);
        try {
            field.field.set(component, assign);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Don't have access to set field " + field.field.getName() + " of "
                    + field.field.getDeclaringClass().getName() + ".");
        }
    }

    private void scanFields() {
        fields = new HashSet<InjectField>();
        Class<?> clazz = component.getClass();

        while (clazz != null) {
            Field[] clazzFields = clazz.getDeclaredFields();

            for (Field clazzField : clazzFields) {
                Parameter parameter = clazzField.getAnnotation(Parameter.class);
                if (parameter != null) {
                    fields.add(new InjectField(clazzField, parameter));
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    private static class InjectField {
        private Field field;
        private Parameter parameter;

        private InjectField(Field field, Parameter parameter) {
            this.field = field;
            this.parameter = parameter;
        }
    }
}
