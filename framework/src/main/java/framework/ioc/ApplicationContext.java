package framework.ioc;

import framework.annotations.Autowired;
import framework.annotations.Component;
import framework.annotations.Controller;
import framework.annotations.Repository;
import framework.routing.ClassPathScanner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ApplicationContext {

    private static final Logger LOGGER = Logger.getLogger(ApplicationContext.class.getName());

    private final Map<Class<?>, Object> beans = new HashMap<>();
    private final List<String> scannedPackages = new ArrayList<>();

    public void registerSingleton(Class<?> type, Object instance) {
        beans.put(type, instance);
        LOGGER.info(() -> "Singleton pré-enregistré : " + type.getName());
    }

    public void scan(String... packages) throws IOException {
        for (String packageName : packages) {
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            scannedPackages.add(packageName.trim());
            List<Class<?>> classes = ClassPathScanner.findClasses(packageName.trim(),
                    Thread.currentThread().getContextClassLoader());
            for (Class<?> clazz : classes) {
                if (isBeanCandidate(clazz)) {
                    registerBean(clazz);
                }
            }
        }
        injectDependencies();
    }

    private boolean isBeanCandidate(Class<?> clazz) {
        return clazz.isAnnotationPresent(Controller.class)
                || clazz.isAnnotationPresent(Component.class)
                || clazz.isAnnotationPresent(Repository.class);
    }

    private void registerBean(Class<?> clazz) {
        if (beans.containsKey(clazz)) {
            return;
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            beans.put(clazz, instance);
            LOGGER.info(() -> "Bean singleton créé : " + clazz.getName());
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le bean " + clazz.getName(), e);
        }
    }

    private void injectDependencies() {
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object dependency = findBeanOfType(field.getType());
                    if (dependency == null) {
                        throw new RuntimeException(
                                "Aucun bean trouvé pour @Autowired " + field.getType().getName()
                                        + " dans " + bean.getClass().getName());
                    }
                    field.setAccessible(true);
                    try {
                        field.set(bean, dependency);
                        LOGGER.info(() -> "Injection @Autowired : "
                                + field.getType().getSimpleName()
                                + " dans " + bean.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(
                                "Erreur d'injection @Autowired dans " + bean.getClass().getName(), e);
                    }
                }
            }
        }
    }

    private Object findBeanOfType(Class<?> type) {
        Object bean = beans.get(type);
        if (bean != null) {
            return bean;
        }
        for (Object instance : beans.values()) {
            if (type.isInstance(instance)) {
                return instance;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        Object bean = beans.get(type);
        if (bean != null) {
            return (T) bean;
        }
        for (Object instance : beans.values()) {
            if (type.isInstance(instance)) {
                return (T) instance;
            }
        }
        return null;
    }

    public boolean containsBean(Class<?> type) {
        return getBean(type) != null;
    }

    public int getBeanCount() {
        return beans.size();
    }

    public Map<Class<?>, Object> getAllBeans() {
        return Map.copyOf(beans);
    }
}
