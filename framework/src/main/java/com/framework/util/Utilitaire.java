package com.framework.util;

import framework.annotations.Controller;
import framework.annotations.Delete;
import framework.annotations.Get;
import framework.annotations.Post;
import framework.annotations.Put;
import framework.annotations.UrlMapping;
import framework.routing.ClassPathScanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.Function;

public class Utilitaire {

    public static void getUrlAndMethod(String pack, HashMap<UtilMethode, Mapping> urlMapping) {
        try {
            for (Class<?> controllerClass : ClassPathScanner.findClasses(pack, Thread.currentThread().getContextClassLoader())) {
                if (controllerClass.isAnnotationPresent(Controller.class)) {
                    registerController(controllerClass, urlMapping);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du scan du package " + pack, e);
        }
    }

    private static void registerController(Class<?> controllerClass, HashMap<UtilMethode, Mapping> urlMapping) {
        Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
        String basePath = normalizePath(controllerAnnotation.value());
        String className = controllerClass.getName();

        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(UrlMapping.class)) {
                UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                String path = combinePaths(basePath, mapping.value());
                String httpMethod = mapping.method().toUpperCase();
                urlMapping.put(new UtilMethode(httpMethod, path), new Mapping(className, method.getName()));
            } else {
                registerMapping(urlMapping, className, method, basePath, Get.class, "GET", Get::value);
                registerMapping(urlMapping, className, method, basePath, Post.class, "POST", Post::value);
                registerMapping(urlMapping, className, method, basePath, Put.class, "PUT", Put::value);
                registerMapping(urlMapping, className, method, basePath, Delete.class, "DELETE", Delete::value);
            }
        }
    }

    private static <A extends Annotation> void registerMapping(
            HashMap<UtilMethode, Mapping> urlMapping,
            String className,
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
        urlMapping.put(new UtilMethode(httpMethod, path), new Mapping(className, method.getName()));
    }

    private static String normalizePath(String path) {
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

    private static String combinePaths(String basePath, String methodPath) {
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
