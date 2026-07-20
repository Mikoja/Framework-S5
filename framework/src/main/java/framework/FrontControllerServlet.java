package framework;

import framework.model.Model;
import framework.model.ModelAndView;
import framework.routing.ControllerScanner;
import framework.routing.RouteMapping;
import framework.routing.RouteRegistry;
import framework.view.ControllerListingRenderer;
import framework.view.ViewResolver;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FrontControllerServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FrontControllerServlet.class.getName());

    private RouteRegistry routeRegistry;
    private ViewResolver viewResolver;

    @Override
    public void init() throws ServletException {
        String controllerPackage = getInitParameter("controllerPackage");
        if (controllerPackage == null || controllerPackage.isBlank()) {
            controllerPackage = getServletContext().getInitParameter("controllerPackage");
        }

        if (controllerPackage == null || controllerPackage.isBlank()) {
            throw new ServletException("Le paramètre controllerPackage doit être configuré dans web.xml");
        }

        String prefix = getServletContext().getInitParameter("viewPrefix");
        String suffix = getServletContext().getInitParameter("viewSuffix");
        viewResolver = new ViewResolver(prefix, suffix);

        try {
            routeRegistry = new ControllerScanner(getClass().getClassLoader()).scan(controllerPackage.trim());
            LOGGER.info(() -> "Routes enregistrées : " + routeRegistry.size());
            for (RouteMapping route : routeRegistry.getAllRoutes()) {
                LOGGER.info(() -> route.httpMethod() + " " + route.path()
                        + " -> " + route.controllerClass().getSimpleName()
                        + "#" + route.handlerMethod().getName());
            }
        } catch (IOException e) {
            throw new ServletException("Impossible de scanner les contrôleurs dans le package " + controllerPackage, e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = extractPath(req);
        String method = req.getMethod();

        resp.setContentType("text/html; charset=UTF-8");

        var route = routeRegistry.find(method, path);
        if (route.isPresent()) {
            if ("GET".equals(method) && "/".equals(path)) {
                ControllerListingRenderer.render(resp.getWriter(), routeRegistry, req.getContextPath());
                return;
            }
            invokeAndDispatch(resp, req, route.get());
            return;
        }

        List<RouteMapping> routesForPath = routeRegistry.findByPath(path);
        if (!routesForPath.isEmpty()) {
            writeMethodNotAllowed(resp, method, path, routesForPath);
            return;
        }

        writeUnknownUrl(resp, req, method, path);
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }

    private void invokeAndDispatch(HttpServletResponse resp, HttpServletRequest req, RouteMapping route) throws IOException {
        try {
            Method handlerMethod = route.handlerMethod();
            Object controller = route.controllerClass().getDeclaredConstructor().newInstance();

            Model model = null;
            boolean hasModelParam = false;

            for (Parameter param : handlerMethod.getParameters()) {
                if (param.getType() == Model.class) {
                    model = new Model();
                    hasModelParam = true;
                    break;
                }
            }

            Object result;
            if (hasModelParam) {
                result = handlerMethod.invoke(controller, model);
            } else {
                result = handlerMethod.invoke(controller);
            }

            if (result instanceof ModelAndView mav) {
                String viewName = mav.getViewName();
                Map<String, Object> mavModel = mav.getModel();
                dispatchView(resp, req, viewName, mavModel);
            } else if (result instanceof String viewName) {
                if (hasModelParam && model != null) {
                    dispatchView(resp, req, viewName, model.getAttributes());
                } else {
                    writeDirectResult(resp, req, route, viewName);
                }
            } else {
                resp.getWriter().write("<p><a href=\"" + escapeHtml(req.getContextPath() + "/") + "\">Accueil</a></p>");
                resp.getWriter().write("<p>Contrôleur : " + escapeHtml(route.controllerClass().getSimpleName()) + "</p>");
                resp.getWriter().write("<p>Méthode : " + escapeHtml(handlerMethod.getName()) + "</p>");
                resp.getWriter().write("<p>URL : " + escapeHtml(route.httpMethod() + " " + route.path()) + "</p>");
                resp.getWriter().write("<p>Résultat : " + escapeHtml(String.valueOf(result)) + "</p>");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Erreur lors de l'invocation de " + route.handlerMethod().getName() + " : " + e.getMessage());
            resp.getWriter().write("<p>Erreur lors de l'exécution de la méthode : " + escapeHtml(e.getMessage()) + "</p>");
        }
    }

    private void dispatchView(HttpServletResponse resp, HttpServletRequest req, String viewName, Map<String, Object> model) throws IOException {
        try {
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }

            String resolvedPath = viewResolver.resolve(viewName);
            RequestDispatcher dispatcher = req.getRequestDispatcher(resolvedPath);
            dispatcher.forward(req, resp);
        } catch (ServletException e) {
            LOGGER.severe(() -> "Erreur lors du forward vers la vue : " + e.getMessage());
            resp.getWriter().write("<p>Erreur lors du dispatch vers la vue : " + escapeHtml(viewName) + "</p>");
        }
    }

    private void writeDirectResult(HttpServletResponse resp, HttpServletRequest req, RouteMapping route, String viewName) throws IOException {
        resp.getWriter().write("<p><a href=\"" + escapeHtml(req.getContextPath() + "/") + "\">Accueil</a></p>");
        resp.getWriter().write("<p>Contrôleur : " + escapeHtml(route.controllerClass().getSimpleName()) + "</p>");
        resp.getWriter().write("<p>Méthode : " + escapeHtml(route.handlerMethod().getName()) + "</p>");
        resp.getWriter().write("<p>URL : " + escapeHtml(route.httpMethod() + " " + route.path()) + "</p>");
        resp.getWriter().write("<p>Résultat : " + escapeHtml(viewName) + "</p>");
    }

    private void writeUnknownUrl(HttpServletResponse resp, HttpServletRequest req, String method, String path) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("<p>URL inconnue : " + escapeHtml(method + " " + path) + "</p>");
        resp.getWriter().write("<p>Voici les URLs disponibles :</p><ul>");
        for (RouteMapping route : routeRegistry.getAllRoutes()) {
            resp.getWriter().write("<li>");
            if ("GET".equals(route.httpMethod())) {
                resp.getWriter().write("<a href=\"" + escapeHtml(buildUrl(req.getContextPath(), route.path())) + "\">");
                resp.getWriter().write(escapeHtml(route.httpMethod() + " " + route.path()));
                resp.getWriter().write("</a>");
            } else {
                resp.getWriter().write(escapeHtml(route.httpMethod() + " " + route.path()));
            }
            resp.getWriter().write("</li>");
        }
        resp.getWriter().write("</ul>");
        resp.getWriter().write("<p><a href=\"" + escapeHtml(req.getContextPath() + "/") + "\">Accueil</a></p>");
    }

    private void writeMethodNotAllowed(HttpServletResponse resp, String method, String path, List<RouteMapping> routes) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().write("<p>405 — " + escapeHtml(method + " " + path) + " non autorisé</p><ul>");
        for (RouteMapping route : routes) {
            resp.getWriter().write("<li>" + escapeHtml(route.httpMethod() + " " + route.path()) + "</li>");
        }
        resp.getWriter().write("</ul>");
    }

    private String extractPath(HttpServletRequest req) {
        String path = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        if (path.isEmpty()) {
            return "/";
        }

        return path;
    }

    private String buildUrl(String contextPath, String path) {
        String base = contextPath == null ? "" : contextPath;
        if ("/".equals(path)) {
            return base.isEmpty() ? "/" : base + "/";
        }
        return base + path;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
