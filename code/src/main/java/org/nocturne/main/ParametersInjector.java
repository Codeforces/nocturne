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

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
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

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = null;
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof Parameter) {
                    parameter = (Parameter) parameterAnnotations[i][j];
                }
            }
            if (parameter == null) {
                throw new ConfigurationException("Each parameter of the method " + method.getDeclaringClass().getName()
                        + "#" + method.getName() + " should be annotated with @Parameter.");
            }
            if (StringUtil.isEmptyOrNull(parameter.name())) {
                throw new ConfigurationException("Each @Parameter in the method " + method.getDeclaringClass().getName()
                        + "#" + method.getName() + " should have name.");
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
        Map<String, String> overrideParameters =
                ApplicationContext.getInstance().getRequestOverrideParameters();
        Map<String, String> requestParameters =
                RequestUtil.getRequestParams(request);

        for (InjectField field : fields) {
            String key = field.parameter.name().isEmpty()
                    ? field.field.getName() : field.parameter.name();

            String value;
            if (overrideParameters != null && overrideParameters.containsKey(key)) {
                value = overrideParameters.get(key);
            } else {
                value = requestParameters.get(key);
            }

            setupField(field, value);
        }

        if (component instanceof Component) {
            Component comp = (Component) component;
            if (overrideParameters != null) {
                for (Map.Entry<String, String> entry : overrideParameters.entrySet()) {
                    comp.addOverrideParameter(entry.getKey(), entry.getValue());
                }
            }
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
        Class<?> clazz = getFieldType(field);

        Object assign = null;
        boolean processed = false;

        if (clazz.equals(String.class)) {
            processed = true;
            assign = value;
        }

        if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            processed = true;
            if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)
                    || "yes".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value)) {
                assign = true;
            } else {
                try {
                    assign = Boolean.valueOf(value);
                } catch (Exception ignored) {
                    assign = false;
                }
            }
        }

        if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            processed = true;
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    assign = Integer.valueOf(value);
                } else {
                    assign = 0;
                }
            } catch (Exception ignored) {
                assign = 0;
            }
        }

        if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            processed = true;
            try {
                if (INTEGRAL_VALUE_PATTERN.matcher(value).matches()) {
                    assign = Long.valueOf(value);
                } else {
                    assign = 0L;
                }
            } catch (Exception ignored) {
                assign = 0L;
            }
        }

        if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            processed = true;
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    assign = Double.valueOf(value);
                } else {
                    assign = 0.0;
                }
            } catch (Exception ignored) {
                assign = 0.0;
            }
        }

        if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            processed = true;
            try {
                if (REAL_VALUE_PATTERN.matcher(value).matches()) {
                    assign = Float.valueOf(value);
                } else {
                    assign = 0.0F;
                }
            } catch (Exception ignored) {
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

        if (processed) {
            setFieldValue(field, assign);
        } else {
            throw new ConfigurationException("Field " + field.field.getName() + " of "
                    + field.field.getDeclaringClass().getName() + " has unexpected type " + clazz.getName() + '.');
        }
    }

    private void setupFieldFromNull(InjectField field) {
        Class<?> clazz = getFieldType(field);

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
            assign = Boolean.FALSE;
        }

        if (clazz.equals(int.class)) {
            assign = 0;
        }

        if (clazz.equals(long.class)) {
            assign = 0L;
        }

        if (clazz.equals(double.class)) {
            assign = 0.0D;
        }

        if (clazz.equals(float.class)) {
            assign = 0.0F;
        }

        setFieldValue(field, assign);
    }

    private Class<?> getFieldType(InjectField field) {
        return field.field != null ? field.field.getType() : field.nonFieldType;
    }

    private void setFieldValue(InjectField field, Object assign) {
        if (field.field != null) {
            field.field.setAccessible(true);
            try {
                field.field.set(component, assign);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Don't have access to set field " + field.field.getName() + " of "
                        + field.field.getDeclaringClass().getName() + '.', e);
            }
        } else {
            field.nonFieldValue = assign;
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
        private final Field field;
        private final Parameter parameter;

        private Object nonFieldValue;
        private Class<?> nonFieldType;

        private InjectField(Field field, Parameter parameter) {
            this.field = field;
            this.parameter = parameter;
        }
    }
}
