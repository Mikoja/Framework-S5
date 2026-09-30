package framework.response;

import framework.routing.RouteMapping;

import java.util.List;

/**
 * Chaîne de responsabilité associant chaque route à la stratégie qui l'écrit.
 * Le premier résolveur qui accepte la route l'emporte ; le dernier est donc le
 * comportement par défaut.
 */
public class ResponseResolverRegistry {

    private final List<ResponseResolver> resolvers;

    public ResponseResolverRegistry(ResponseResolver... resolvers) {
        this(List.of(resolvers));
    }

    public ResponseResolverRegistry(List<ResponseResolver> resolvers) {
        if (resolvers == null || resolvers.isEmpty()) {
            throw new IllegalArgumentException("Au moins un ResponseResolver doit être fourni");
        }
        this.resolvers = List.copyOf(resolvers);
    }

    public ResponseResolver forRoute(RouteMapping route) {
        for (ResponseResolver resolver : resolvers) {
            if (resolver.supports(route)) {
                return resolver;
            }
        }
        throw new IllegalStateException("Aucune stratégie de réponse ne gère la route " + route.key());
    }

    public List<ResponseResolver> getResolvers() {
        return resolvers;
    }
}
