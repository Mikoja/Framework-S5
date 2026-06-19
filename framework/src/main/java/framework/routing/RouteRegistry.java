package framework.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RouteRegistry {

    private final Map<String, RouteMapping> routes = new LinkedHashMap<>();

    public void register(RouteMapping mapping) {
        String key = mapping.key();
        if (routes.containsKey(key)) {
            RouteMapping existing = routes.get(key);
            throw new IllegalStateException(
                    "Route en conflit pour " + key + " : "
                            + existing.controllerClass().getName() + "#" + existing.handlerMethod().getName()
                            + " et "
                            + mapping.controllerClass().getName() + "#" + mapping.handlerMethod().getName()
            );
        }
        routes.put(key, mapping);
    }

    public Optional<RouteMapping> find(String httpMethod, String path) {
        return Optional.ofNullable(routes.get(httpMethod.toUpperCase() + ":" + path));
    }

    public List<RouteMapping> findByPath(String path) {
        return routes.values().stream()
                .filter(route -> route.path().equals(path))
                .toList();
    }

    public Optional<RouteMapping> findSimilarPath(String path) {
        return routes.values().stream()
                .filter(route -> route.path().endsWith(path) && !route.path().equals(path))
                .findFirst();
    }

    public Collection<RouteMapping> getAllRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }

    public Map<Class<?>, List<RouteMapping>> getRoutesByController() {
        Map<Class<?>, List<RouteMapping>> grouped = new LinkedHashMap<>();
        for (RouteMapping route : routes.values()) {
            grouped.computeIfAbsent(route.controllerClass(), controller -> new ArrayList<>()).add(route);
        }
        return grouped;
    }

    public int getControllerCount() {
        return getRoutesByController().size();
    }

    public int size() {
        return routes.size();
    }
}
