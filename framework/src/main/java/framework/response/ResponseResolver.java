package framework.response;

import framework.model.Model;
import framework.routing.RouteMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Stratégie d'écriture de la réponse HTTP pour une route donnée.
 *
 * <p>Permet d'ajouter un format de sortie (JSON, XML, texte brut...) sans
 * toucher au front controller. La sélection se fait sur la route et non sur
 * le type de retour, afin que la gestion d'erreur soit déléguée à la même
 * stratégie, y compris quand l'action a levé une exception.</p>
 */
public interface ResponseResolver {

    boolean supports(RouteMapping route);

    /**
     * @param model modèle construit par l'action, {@code null} si l'action ne
     *             déclare pas de paramètre {@link Model}
     */
    void resolve(HttpServletRequest request,
                 HttpServletResponse response,
                 RouteMapping route,
                 Model model,
                 Object result) throws IOException;

    void resolveError(HttpServletRequest request,
                      HttpServletResponse response,
                      RouteMapping route,
                      Throwable error) throws IOException;
}
