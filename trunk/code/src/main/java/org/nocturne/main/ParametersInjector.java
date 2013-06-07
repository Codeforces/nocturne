/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import net.sf.cglib.reflect.FastMethod;
import org.nocturne.annotation.Parameter;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.util.RequestUtil;
import org.nocturne.util.StringUtil;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

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
    private static final Pattern INTEGRAL_VALUE_PATTERN = Pattern.compile("0|(-?[1-9][0-9]*)");
    private static final Pattern REAL_VALUE_PATTERN = Pattern.compile("(0|(-?[1-9][0-9]*))((\\.[0-9]+)?)");

    /**
     * Injection target object.
     */
    private final Object component;

    /**
     * Stores information about
     */
    private Set<InjectField> fields;

    /**
     * @param component Object which has fields with @Parameter annotation.
     */
    public ParametersInjector(Object component) {
        this.component = component;
    }

    /**
     * @param request Request to be analyzed to find parameters for injection.
     *                Also more priority parameters are retrieved from ApplicationContext.getInstance().getRequestOverrideParameters().
     */
    public void inject(HttpServletRequest request) {
        if (fields == null) {
            scanFields();
        }

        setupFields(request, fields);
    }

    /**
     * Returns parameter values, all parameters expected to be annotated with named @Parameter.
     *
     * @param request Http request.
     * @param method  Method which parameters will be analyzed to assign values.
     * @return Object[] containing values for method parameters from the http request.
     */
    public Object[] setupParameters(HttpServletRequest request, FastMethod method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getJavaMethod().getParameterAnnotations();

        if (parameterTypes.length != parameterAnnotations.length) {
            throw new NocturneException("Expected the same number of parameters and annotations.");
        }

        List<InjectField> injectFields = new ArrayList<InjectField>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = null;
            for (int j = 0; j < parameterAnnotations[i].length; ++j) {
                if (parameterAnnotations[i][j] instanceof Parameter) {
                    parameter = (Parameter) parameterAnnotations[i][j];
                }
            }
            if (parameter == null) {
                throw new ConfigurationException("Each parameter of the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should be annotated with @Parameter.");
            }
            if (StringUtil.isEmptyOrNull(parameter.name())) {
                throw new ConfigurationException("Each @Parameter in the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should have name.");
            }
            InjectField injectField = new InjectField(null, parameter);
            injectField.nonFieldType = parameterType;
            injectFields.add(injectField);
        }

        setupFields(request, injectFields);

        Object[] result = new Object[injectFields.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = injectFields.get(i).nonFieldValue;
        }

        return result;
    }

    private void setupFields(HttpServletRequest request, Collection<InjectField> fields) {
        Map<String, List<String>> overrideParameters =
                ApplicationContext.getInstance().getRequestOverrideParameters();
        Map<String, List<String>> requestParameters = RequestUtil.getRequestParams(request);

        for (InjectField field : fields) {
            String key = field.parameter.name().isEmpty()
                    ? field.field.getName() : field.parameter.name();

            List<String> values;
            if (overrideParameters != null && overrideParameters.containsKey(key)) {
                values = overrideParameters.get(key);
            } else {
                values = requestParameters.get(key);
            }

            setupField(field, values);
        }

        if (component instanceof Component) {
            Component comp = (Component) component;
            if (overrideParameters != null) {
                for (Map.Entry<String, List<String>> entry : overrideParameters.entrySet()) {
                    comp.addOverrideParameter(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void setupField(InjectField field, @Nullable List<String> values) {
        Class<?> fieldType = getFieldType(field);

        if (fieldType.isArray()) {
            setFieldValue(field, getArrayAssignValue(field, values, fieldType));
        } else {
            String value = RequestUtil.getFirst(values);

            if (value == null) {
                setFieldValue(field, getNullAssignValue(fieldType));
            } else {
                value = field.parameter.stripMode().strip(value);
                setFieldValue(field, getAssignValue(field, value, fieldType));
            }
        }
    }

    public static Object getArrayAssignValue(@Nullable InjectField field, List<String> values, Class<?> fieldType) {
        Class<?> componentType = fieldType.getComponentType();

        if (componentType.isArray()) {
            throw getIllegalFieldTypeException(field, fieldType);
        }

        int valueCount = values == null || values.isEmpty() ? 0 : values.size();
        Object fieldValue = Array.newInstance(componentType, valueCount);

        for (int valueIndex = 0; valueIndex < valueCount; ++valueIndex) {
            String value = values.get(valueIndex);

            if (value == null) {
                Array.set(fieldValue, valueIndex, getNullAssignValue(componentType));
            } else {
                if (field != null) {
                    value = field.parameter.stripMode().strip(value);
                }
                Array.set(fieldValue, valueIndex, getAssignValue(field, value, componentType));
            }
        }

        return fieldValue;
    }

    private static Object getNullAssignValue(Class<?> fieldType) {
        if (String.class.equals(fieldType) || Boolean.class.equals(fieldType)
                || Character.class.equals(fieldType) || Byte.class.equals(fieldType) || Short.class.equals(fieldType)
                || Integer.class.equals(fieldType) || Long.class.equals(fieldType)
                || Float.class.equals(fieldType) || Double.class.equals(fieldType)
                || fieldType.isEnum()) {
            return null;
        }

        if (fieldType.equals(boolean.class)) {
            return Boolean.FALSE;
        }

        if (fieldType.equals(char.class)) {
            return 0;
        }

        if (fieldType.equals(byte.class)) {
            return 0;
        }

        if (fieldType.equals(short.class)) {
            return 0;
        }

        if (fieldType.equals(int.class)) {
            return 0;
        }

        if (fieldType.equals(long.class)) {
            return 0L;
        }

        if (fieldType.equals(float.class)) {
            return 0.0F;
        }

        if (fieldType.equals(double.class)) {
            return 0.0D;
        }

        return null;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    private static Object getAssignValue(@Nullable InjectField field, String value, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return value;
        }

        if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)
                    || "yes".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value)
                    || "y".equalsIgnoreCase(value) || "checked".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else {
                try {
                    return Boolean.valueOf(value);
                } catch (RuntimeException ignored) {
                    return Boolean.FALSE;
                }
            }
        }

        if (targetType.equals(Character.class) || targetType.equals(char.class)) {
            try {
                if (value.isEmpty()) {
                    return (char) 0;
                } else {
                    return value.charAt(0);
                }
            } catch (RuntimeException ignored) {
                return (char) 0;
            }
        }

        if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Byte.valueOf(value);
                } else {
                    return (byte) 0;
                }
            } catch (RuntimeException ignored) {
                return (byte) 0;
            }
        }

        if (targetType.equals(Short.class) || targetType.equals(short.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Short.valueOf(value);
                } else {
                    return (short) 0;
                }
            } catch (RuntimeException ignored) {
                return (short) 0;
            }
        }

        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Integer.valueOf(value);
                } else {
                    return 0;
                }
            } catch (RuntimeException ignored) {
                return 0;
            }
        }

        if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Long.valueOf(value);
                } else {
                    return 0L;
                }
            } catch (RuntimeException ignored) {
                return 0L;
            }
        }

        if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Float.valueOf(value);
                } else {
                    return 0.0F;
                }
            } catch (RuntimeException ignored) {
                return 0.0F;
            }
        }

        if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Double.valueOf(value);
                } else {
                    return 0.0D;
                }
            } catch (RuntimeException ignored) {
                return 0.0D;
            }
        }

        if (targetType.isEnum()) {
            for (Object constName : targetType.getEnumConstants()) {
                if (constName.toString().equalsIgnoreCase(value)) {
                    return constName;
                }
            }
            return null;
        }

        throw getIllegalFieldTypeException(field, targetType);
    }

    private static Class<?> getFieldType(InjectField field) {
        return field.field == null ? field.nonFieldType : field.field.getType();
    }

    private void setFieldValue(InjectField field, Object assign) {
        if (field.field == null) {
            field.nonFieldValue = assign;
        } else {
            field.field.setAccessible(true);
            try {
                field.field.set(component, assign);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(String.format(
                        "Don't have access to set field %s of %s.",
                        field.field.getName(), field.field.getDeclaringClass().getName()
                ), e);
            }
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

    private static ConfigurationException getIllegalFieldTypeException(InjectField field, Class<?> fieldType) {
        if (field == null) {
            return new ConfigurationException(String.format("Field has unexpected type %s.", fieldType.getName()));
        } else {
            return new ConfigurationException(String.format(
                    "Field %s of %s has unexpected type %s.",
                    field.field.getName(), field.field.getDeclaringClass().getName(), fieldType.getName()
            ));
        }
    }

    @SuppressWarnings("PackageVisibleField")
    private static class InjectField {
        final Field field;
        final Parameter parameter;

        Object nonFieldValue;
        Class<?> nonFieldType;

        private InjectField(Field field, Parameter parameter) {
            this.field = field;
            this.parameter = parameter;
        }
    }
}
