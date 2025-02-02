/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.common.base.Preconditions;
import net.sf.cglib.reflect.FastMethod;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.nocturne.annotation.Parameter;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.util.RequestUtil;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * Each component has an private instance of ParametersInjector.
 * It will process @Parameter annotations.
 * </p>
 * <p>
 * Also this class can be used if you want to
 * inject parameters into some object.
 * </p>
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings("WeakerAccess")
public class ParametersInjector {
    private static final Logger logger = Logger.getLogger(ParametersInjector.class);

    private static final Pattern INTEGRAL_VALUE_PATTERN = Pattern.compile("0|(-?[1-9][0-9]*)");
    private static final Pattern REAL_VALUE_PATTERN = Pattern.compile("(0|(-?[1-9][0-9]*))((\\.[0-9]+)?)");

    private static final Character NULL_ASSIGN_CHAR = 0;
    private static final Byte NULL_ASSIGN_BYTE = 0;
    private static final Short NULL_ASSIGN_SHORT = 0;
    private static final Integer NULL_ASSIGN_INT = 0;
    private static final Long NULL_ASSIGN_LONG = 0L;
    private static final Float NULL_ASSIGN_FLOAT = 0.0F;
    private static final Double NULL_ASSIGN_DOUBLE = 0.0D;

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
    Object[] setupParameters(HttpServletRequest request, FastMethod method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getJavaMethod().getParameterAnnotations();

        if (parameterTypes.length != parameterAnnotations.length) {
            logger.error("Expected the same number of parameters and annotations.");
            throw new NocturneException("Expected the same number of parameters and annotations.");
        }

        List<InjectField> injectFields = new ArrayList<>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = null;
            for (int j = 0; j < parameterAnnotations[i].length; ++j) {
                if (parameterAnnotations[i][j] instanceof Parameter) {
                    parameter = (Parameter) parameterAnnotations[i][j];
                }
            }
            if (parameter == null) {
                logger.error("Each parameter of the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should be annotated with @Parameter.");
                throw new ConfigurationException("Each parameter of the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should be annotated with @Parameter.");
            }
            if (StringUtil.isEmpty(parameter.name())) {
                logger.error("Each @Parameter in the method " + method.getDeclaringClass().getName()
                        + '#' + method.getName() + " should have name.");
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
                    ? Preconditions.checkNotNull(field.field).getName() : field.parameter.name();

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

    static Object getArrayAssignValue(
            @Nullable InjectField field, @Nullable List<String> values, Class<?> fieldType) {
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

    @Contract(pure = true)
    @Nullable
    private static Object getNullAssignValue(Class<?> fieldType) {
        if (fieldType == boolean.class) {
            return Boolean.FALSE;
        }

        if (fieldType == char.class) {
            return NULL_ASSIGN_CHAR;
        }

        if (fieldType == byte.class) {
            return NULL_ASSIGN_BYTE;
        }

        if (fieldType == short.class) {
            return NULL_ASSIGN_SHORT;
        }

        if (fieldType == int.class) {
            return NULL_ASSIGN_INT;
        }

        if (fieldType == long.class) {
            return NULL_ASSIGN_LONG;
        }

        if (fieldType == float.class) {
            return NULL_ASSIGN_FLOAT;
        }

        if (fieldType == double.class) {
            return NULL_ASSIGN_DOUBLE;
        }

        return null;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    @Nullable
    private static Object getAssignValue(@Nullable InjectField field, String value, @Nonnull Class<?> targetType) {
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
                    return NULL_ASSIGN_CHAR;
                } else {
                    return value.charAt(0);
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_CHAR;
            }
        }

        if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Byte.valueOf(value);
                } else {
                    return NULL_ASSIGN_BYTE;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_BYTE;
            }
        }

        if (targetType.equals(Short.class) || targetType.equals(short.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Short.valueOf(value);
                } else {
                    return NULL_ASSIGN_SHORT;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_SHORT;
            }
        }

        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Integer.valueOf(value);
                } else {
                    return NULL_ASSIGN_INT;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_INT;
            }
        }

        if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Long.valueOf(value);
                } else {
                    return NULL_ASSIGN_LONG;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_LONG;
            }
        }

        if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Float.valueOf(value);
                } else {
                    return NULL_ASSIGN_FLOAT;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_FLOAT;
            }
        }

        if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    return Double.valueOf(value);
                } else {
                    return NULL_ASSIGN_DOUBLE;
                }
            } catch (RuntimeException ignored) {
                return NULL_ASSIGN_DOUBLE;
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

    @Contract(pure = true)
    private static Class<?> getFieldType(InjectField field) {
        return field.field == null ? field.nonFieldType : field.field.getType();
    }

    private void setFieldValue(InjectField field, @Nullable Object assign) {
        if (field.field == null) {
            field.nonFieldValue = assign;
        } else {
            field.field.setAccessible(true);
            try {
                field.field.set(component, assign);
            } catch (IllegalAccessException e) {
                String message = String.format(
                        "Don't have access to set field %s of %s.",
                        field.field.getName(), field.field.getDeclaringClass().getName()
                );
                logger.error(message, e);
                throw new IllegalArgumentException(message, e);
            }
        }
    }

    private void scanFields() {
        fields = new HashSet<>();
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

    @Nonnull
    private static ConfigurationException getIllegalFieldTypeException(
            @Nullable InjectField field, @Nonnull Class<?> fieldType) {
        if (field == null || field.field == null) {
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
        @Nullable
        final Field field;
        final Parameter parameter;

        Object nonFieldValue;
        Class<?> nonFieldType;

        private InjectField(@Nullable Field field, Parameter parameter) {
            this.field = field;
            this.parameter = parameter;
        }
    }
}
