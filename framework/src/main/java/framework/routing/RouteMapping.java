package framework.routing;

import framework.annotations.Auth;
import framework.annotations.Authenticated;
import framework.annotations.Json;

import java.lang.reflect.Method;

public record RouteMapping(
        String httpMethod,
        String path,
        Class<?> controllerClass,
        Method handlerMethod
) {
    public String key() {
        return httpMethod + ":" + path;
    }

    public Json json() {
        Json onMethod = handlerMethod.getAnnotation(Json.class);
        if (onMethod != null) {
            return onMethod;
        }
        return controllerClass.getAnnotation(Json.class);
    }

    public boolean isJson() {
        return json() != null;
    }

    public boolean requiresAuth() {
        return handlerMethod.isAnnotationPresent(Auth.class)
                || handlerMethod.isAnnotationPresent(Authenticated.class);
    }

    @Override
    public String toString() {
        return key() + " -> " + controllerClass.getSimpleName() + "#" + handlerMethod.getName()
                + (isJson() ? " [JSON]" : " [VIEW]");
    }
}
