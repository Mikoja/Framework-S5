package framework.routing;

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
}
