package framework.routing;

import framework.annotations.Json;

import java.lang.reflect.Method;

/**
 * Métadonnées d'une route.
 *
 * <p>Le {@link Json} éventuel porté par l'action ou par le contrôleur est
 * exposé ici sous forme de méthodes dérivées : le système d'URLmapping reste
 * inchangé, l'annotation ne sert qu'à sélectionner la stratégie de réponse.</p>
 */
public record RouteMapping(
        String httpMethod,
        String path,
        Class<?> controllerClass,
        Method handlerMethod
) {
    public String key() {
        return httpMethod + ":" + path;
    }

    /** @return le {@link Json} de l'action ou du contrôleur, {@code null} si la route renvoie une vue */
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

    @Override
    public String toString() {
        return key() + " -> " + controllerClass.getSimpleName() + "#" + handlerMethod.getName()
                + (isJson() ? " [JSON]" : " [VIEW]");
    }
}
