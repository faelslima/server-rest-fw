package com.logos.restserver.model.lazytype;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xpert.persistence.utils.EntityUtils;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author rafael
 */
public class HibernateProxyTypeAdapter extends TypeAdapter<HibernateProxy> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return (HibernateProxy.class.isAssignableFrom(type.getRawType()) ? (TypeAdapter<T>) new HibernateProxyTypeAdapter(gson) : null);
        }
    };
    private final Gson context;

    private HibernateProxyTypeAdapter(Gson context) {
        this.context = context;
    }

    @Override
    public HibernateProxy read(JsonReader in) throws IOException {
        throw new UnsupportedOperationException("Not supported");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(JsonWriter out, HibernateProxy value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        // Retrieve the original (not proxy) class
        Class<?> baseType = Hibernate.getClass(value);
        // Get the TypeAdapter of the original class, to delegate the serialization
        TypeAdapter delegate = context.getAdapter(TypeToken.get(baseType));
        // Get a filled instance of the original class
        Object object = ((HibernateProxy) value).getHibernateLazyInitializer()
                .getImplementation();

        Class classe = object.getClass();
        Class superClasse = classe.getSuperclass();

        Method[] methods = classe.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && method.getModifiers() == Modifier.PUBLIC && method.getParameters().length == 0) {
                Object child;
                try {
                    child = method.invoke(object);
                    if (child != null && child instanceof HibernateProxy) {
                        Object desserializedObject = ((HibernateProxy) child)
                                .getHibernateLazyInitializer()
                                .getImplementation();
                        Object newValue = readLazyChild(desserializedObject);
                        String setMethodName = methodName.replaceFirst("get", "set");

                        Method setValue = object.getClass().getMethod(setMethodName, desserializedObject.getClass());
                        if (newValue != null) {
                            setValue.invoke(object, newValue);
                        } else {
                            setValue.invoke(object, desserializedObject.getClass().cast(null));
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
                    Logger.getLogger(HibernateProxyTypeAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        methods = superClasse.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && method.getModifiers() == Modifier.PUBLIC && method.getParameters().length == 0) {
                Object child;
                try {
                    child = method.invoke(object);
                    if (child != null && child instanceof HibernateProxy) {
                        Object desserializedObject = ((HibernateProxy) child)
                                .getHibernateLazyInitializer()
                                .getImplementation();
                        Object newValue = readLazyChild(desserializedObject);
                        String setMethodName = methodName.replaceFirst("get", "set");

                        Method setValue = object.getClass().getSuperclass().getMethod(setMethodName, desserializedObject.getClass());
                        if (newValue != null) {
                            setValue.invoke(object, newValue);
                        } else {
                            setValue.invoke(object, desserializedObject.getClass().cast(null));
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
                    Logger.getLogger(HibernateProxyTypeAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        // Serialize the value
        delegate.write(out, object);
    }

    private Object readLazyChild(Object child) {

        Object childClass = null;
        Method methodSetId = null;
        String fieldId = EntityUtils.getIdFieldName(child);

        if (EntityUtils.getIdType(child.getClass()) == Long.class) {
            try {
                methodSetId = child.getClass().getMethod("set" + getMethodName(fieldId), Long.class);
            } catch (NoSuchMethodException ex) {
                try {
                    methodSetId = child.getClass().getSuperclass().getMethod("set" + getMethodName(fieldId), Long.class);
                } catch (NoSuchMethodException ex1) {
                    Logger.getLogger(HibernateProxyTypeAdapter.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        if (EntityUtils.getIdType(child.getClass()) == String.class) {
            try {
                methodSetId = child.getClass().getMethod("set" + getMethodName(fieldId), String.class);
            } catch (NoSuchMethodException ex) {
                try {
                    methodSetId = child.getClass().getSuperclass().getMethod("set" + getMethodName(fieldId), String.class);
                } catch (NoSuchMethodException ex1) {
                    Logger.getLogger(HibernateProxyTypeAdapter.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            if (methodSetId != null) {
                try {
                    childClass = child.getClass().newInstance();
                    methodSetId.invoke(childClass, EntityUtils.getId(child));

                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(HibernateProxyTypeAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return childClass;
    }

    private String getMethodName(String fieldName) {
        String firstLetter = fieldName.substring(0, 1).toUpperCase();
        return firstLetter + fieldName.substring(1);
    }
}
