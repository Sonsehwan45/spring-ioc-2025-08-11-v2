package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    private Set<Class<?>> componentClasses;
    private final Map<String, Object> beans = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);
        this.componentClasses = reflections.getTypesAnnotatedWith(Component.class);
        createBeans();
    }

    private void createBeans() {
        for (Class<?> cls : componentClasses) {
            if (cls.isInterface()) continue;

            Constructor<?>[] constructors = cls.getDeclaredConstructors();

            if (constructors.length == 1 && constructors[0].getParameterCount() == 0) {
                createBean(cls);
            }
        }

        for (Class<?> cls : componentClasses) {
            if (cls.isInterface()) continue;

            String beanName = Ut.str.lcfirst(cls.getSimpleName());

            if (!beans.containsKey(beanName)) {
                createBean(cls);
            }
        }
    }

    private void createBean(Class<?> cls) {
        try {
            Constructor<?> constructor = cls.getDeclaredConstructors()[0];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                String dependencyBeanName = Ut.str.lcfirst(parameterTypes[i].getSimpleName());
                args[i] = beans.get(dependencyBeanName);
            }

            Object instance = constructor.newInstance(args);
            String beanName = Ut.str.lcfirst(cls.getSimpleName());
            beans.put(beanName, instance);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }
}