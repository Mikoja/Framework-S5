package framework;

import framework.routing.ControllerScanner;
import framework.routing.RouteMapping;
import framework.routing.RouteRegistry;
import framework.view.ControllerListingRenderer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class FrontControllerServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FrontControllerServlet.class.getName());

    private RouteRegistry routeRegistry;

    @Override
    public void init() throws ServletException {
        String controllerPackage = getInitParameter("controllerPackage");
        if (controllerPackage == null || controllerPackage.isBlank()) {
            controllerPackage = getServletContext().getInitParameter("controllerPackage");
        }

        if (controllerPackage == null || controllerPackage.isBlank()) {
            throw new ServletException("Le paramètre controllerPackage doit être configuré dans web.xml");
        }

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
            writeMatchedRoute(resp, req, route.get());
            return;
        }

        List<RouteMapping> routesForPath = routeRegistry.findByPath(path);
        if (!routesForPath.isEmpty()) {
            writeMethodNotAllowed(resp, method, path, routesForPath);
            return;
        }

        writeNotFound(resp, req, method, path);
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
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

    private void writeMatchedRoute(HttpServletResponse resp, HttpServletRequest req, RouteMapping route) throws IOException {
        resp.getWriter().write("<p><a href=\"" + escapeHtml(req.getContextPath() + "/") + "\">Accueil</a></p>");
        resp.getWriter().write("<p>" + escapeHtml(route.httpMethod() + " " + route.path()) + "</p>");

        if ("GET".equals(route.httpMethod())) {
            boolean hasPostRoute = routeRegistry.findByPath(route.path()).stream()
                    .anyMatch(candidate -> "POST".equals(candidate.httpMethod()));
            if (hasPostRoute) {
                resp.getWriter().write("<form method=\"post\"><button type=\"submit\">POST</button></form>");
            }
        }
    }

    private void writeMethodNotAllowed(HttpServletResponse resp, String method, String path, List<RouteMapping> routes) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().write("<p>405 — " + escapeHtml(method + " " + path) + " non autorisé</p><ul>");
        for (RouteMapping route : routes) {
            resp.getWriter().write("<li>" + escapeHtml(route.httpMethod() + " " + route.path()) + "</li>");
        }
        resp.getWriter().write("</ul>");
    }

    private void writeNotFound(HttpServletResponse resp, HttpServletRequest req, String method, String path) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("<p>404 — " + escapeHtml(method + " " + path) + " introuvable</p>");
        resp.getWriter().write("<p><a href=\"" + escapeHtml(req.getContextPath() + "/") + "\">Accueil</a></p>");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
