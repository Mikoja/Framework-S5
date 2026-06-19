package framework.routing;

import framework.annotations.Controller;
import framework.annotations.Delete;
import framework.annotations.Get;
import framework.annotations.Post;
import framework.annotations.Put;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Function;

public class ControllerScanner {

    private final ClassLoader classLoader;

    public ControllerScanner() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ControllerScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public RouteRegistry scan(String basePackage) throws IOException {
        RouteRegistry registry = new RouteRegistry();

        for (Class<?> controllerClass : ClassPathScanner.findClasses(basePackage, classLoader)) {
            if (controllerClass.isAnnotationPresent(Controller.class)) {
                registerController(registry, controllerClass);
            }
        }

        return registry;
    }

    private void registerController(RouteRegistry registry, Class<?> controllerClass) {
        Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
        String basePath = normalizePath(controllerAnnotation.value());

        for (Method method : controllerClass.getDeclaredMethods()) {
            registerMapping(registry, controllerClass, method, basePath, Get.class, "GET", Get::value);
            registerMapping(registry, controllerClass, method, basePath, Post.class, "POST", Post::value);
            registerMapping(registry, controllerClass, method, basePath, Put.class, "PUT", Put::value);
            registerMapping(registry, controllerClass, method, basePath, Delete.class, "DELETE", Delete::value);
        }
    }

    private <A extends Annotation> void registerMapping(
            RouteRegistry registry,
            Class<?> controllerClass,
            Method method,
            String basePath,
            Class<A> annotationType,
            String httpMethod,
            Function<A, String> pathExtractor
    ) {
        if (!method.isAnnotationPresent(annotationType)) {
            return;
        }

        A annotation = method.getAnnotation(annotationType);
        String path = combinePaths(basePath, pathExtractor.apply(annotation));
        RouteMapping mapping = new RouteMapping(httpMethod, path, controllerClass, method);
        registry.register(mapping);
    }

    static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    static String combinePaths(String basePath, String methodPath) {
        String normalizedBase = normalizePath(basePath);
        String normalizedMethod = methodPath == null || methodPath.isBlank() ? "" : methodPath.trim();

        if (normalizedMethod.isEmpty()) {
            return normalizedBase;
        }

        if (!normalizedMethod.startsWith("/")) {
            normalizedMethod = "/" + normalizedMethod;
        }

        if ("/".equals(normalizedBase)) {
            return normalizePath(normalizedMethod);
        }

        return normalizePath(normalizedBase + normalizedMethod);
    }
}
