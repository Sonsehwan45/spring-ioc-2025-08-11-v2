package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Repository;
import com.ll.framework.ioc.annotations.Service;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    private final Map<String, Object> beanMap = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
        init();
    }

    public void init() {
        // 외부 라이브러리 reflections를 사용해 basePackage 내부 모든 클래스를 돌며 자동으로 bean 등록
        Reflections reflections = new Reflections(basePackage);

        // @어노테이션이 붙은 클래스들을 찾음
        Set<Class<?>> componentClasses = new HashSet<>();
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Service.class));
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Repository.class));

        // 1차로 모든 bean 이름만 등록 (순환 참조 대비)
        for (Class<?> clazz : componentClasses) {
            if (Modifier.isAbstract(clazz.getModifiers())) continue;
            String beanName = lowerFirstChar(clazz.getSimpleName());
            beanMap.put(beanName, null); // placeholder
        }

        // 2차로 실제 인스턴스 생성
        for (Class<?> clazz : componentClasses) {
            if (Modifier.isAbstract(clazz.getModifiers())) continue;
            String beanName = lowerFirstChar(clazz.getSimpleName());
            if (beanMap.get(beanName) == null) { // 아직 인스턴스 안만들었으면 생성
                Object instance = createBean(clazz);
                beanMap.put(beanName, instance);
            }
        }
    }

    private Object createBean(Class<?> clazz) {
        try {
            // 기본 생성자 있는 경우
            try {
                Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
                return defaultConstructor.newInstance();
            } catch (NoSuchMethodException ignored) {
                // 기본 생성자가 없는 경우 → 가장 긴 생성자를 사용하여 의존성 주입
                Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                Constructor<?> constructor = constructors[0]; // 하나만 있다고 가정 (lombok @RequiredArgsConstructor 상황)

                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] params = new Object[paramTypes.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    String depBeanName = lowerFirstChar(paramTypes[i].getSimpleName());
                    Object depBean = genBean(depBeanName);
                    if (depBean == null) {
                        depBean = createBean(paramTypes[i]); // 아직 안만들었으면 재귀 생성
                        beanMap.put(depBeanName, depBean);
                    }
                    params[i] = depBean;
                }
                return constructor.newInstance(params);
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 생성 실패: " + clazz.getName(), e);
        }
    }

    public <T> T genBean(String beanName) {
        return (T) beanMap.get(beanName);
    }

    private String lowerFirstChar(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }
}