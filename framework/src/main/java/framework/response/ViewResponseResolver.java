package framework.response;

import framework.model.Model;
import framework.model.ModelAndView;
import framework.routing.RouteMapping;
import framework.view.HtmlUtils;
import framework.view.ViewResolver;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Comportement historique du framework : la valeur de retour est interprétée
 * comme le nom d'une vue vers laquelle la requête est redirigée en interne.
 *
 * <p>Un {@link ModelAndView} fournit sa vue et son modèle, une {@link String}
 * est dispatchée si l'action construit un {@link Model}, et toute autre valeur
 * donne lieu à une page de mise au point.</p>
 */
public class ViewResponseResolver implements ResponseResolver {

    public static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ViewResolver viewResolver;

    public ViewResponseResolver(ViewResolver viewResolver) {
        this.viewResolver = viewResolver != null ? viewResolver : new ViewResolver("", "");
    }

    @Override
    public boolean supports(RouteMapping route) {
        return route != null && !route.isJson();
    }

    @Override
    public void resolve(HttpServletRequest request,
                        HttpServletResponse response,
                        RouteMapping route,
                        Model model,
                        Object result) throws IOException {
        response.setContentType(HTML_CONTENT_TYPE);

        if (result instanceof ModelAndView modelAndView) {
            forward(request, response, modelAndView.getViewName(), modelAndView.getModel());
            return;
        }

        if (result instanceof String viewName) {
            if (model != null) {
                forward(request, response, viewName, model.getAttributes());
            } else {
                writeDebugPage(request, response, route, viewName);
            }
            return;
        }

        writeDebugPage(request, response, route, String.valueOf(result));
    }

    @Override
    public void resolveError(HttpServletRequest request,
                             HttpServletResponse response,
                             RouteMapping route,
                             Throwable error) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(HTML_CONTENT_TYPE);
        response.getWriter().write(
                "<p>Erreur lors de l'exécution de la méthode : " + HtmlUtils.escape(messageOf(error)) + "</p>");
    }

    private void forward(HttpServletRequest request,
                         HttpServletResponse response,
                         String viewName,
                         Map<String, Object> model) throws IOException {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        try {
            String resolvedPath = viewResolver.resolve(viewName);
            RequestDispatcher dispatcher = request.getRequestDispatcher(resolvedPath);
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            if (response.isCommitted()) {
                return;
            }
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(HTML_CONTENT_TYPE);
            response.getWriter().write(
                    "<p>Erreur lors du dispatch vers la vue : " + HtmlUtils.escape(viewName) + "</p>");
        }
    }

    private void writeDebugPage(HttpServletRequest request,
                                HttpServletResponse response,
                                RouteMapping route,
                                String result) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.write("<p><a href=\"" + HtmlUtils.escape(request.getContextPath() + "/") + "\">Accueil</a></p>");
        writer.write("<p>Contrôleur : " + HtmlUtils.escape(route.controllerClass().getSimpleName()) + "</p>");
        writer.write("<p>Méthode : " + HtmlUtils.escape(route.handlerMethod().getName()) + "</p>");
        writer.write("<p>URL : " + HtmlUtils.escape(route.httpMethod() + " " + route.path()) + "</p>");
        writer.write("<p>Résultat : " + HtmlUtils.escape(result) + "</p>");
    }

    private String messageOf(Throwable error) {
        if (error == null) {
            return "Erreur inconnue";
        }
        return error.getMessage() != null ? error.getMessage() : error.getClass().getName();
    }
}
