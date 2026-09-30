package framework;

import framework.ioc.ApplicationContext;
import framework.json.ReflectiveJsonSerializer;
import framework.model.Model;
import framework.persistence.ConnectionFactory;
import framework.persistence.DatabaseConfig;
import framework.response.JsonResponseResolver;
import framework.response.ResponseResolver;
import framework.response.ResponseResolverRegistry;
import framework.response.ViewResponseResolver;
import framework.routing.ControllerScanner;
import framework.routing.RouteMapping;
import framework.routing.RouteRegistry;
import framework.view.ControllerListingRenderer;
import framework.view.HtmlUtils;
import framework.view.ViewResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FrontControllerServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FrontControllerServlet.class.getName());

    private RouteRegistry routeRegistry;
    private ViewResolver viewResolver;
    private ApplicationContext applicationContext;
    private ConnectionFactory connectionFactory;
    private ResponseResolverRegistry responseResolvers;

    @Override
    public void init() throws ServletException {
        String controllerPackage = getInitParameter("controllerPackage");
        if (controllerPackage == null || controllerPackage.isBlank()) {
            controllerPackage = getServletContext().getInitParameter("controllerPackage");
        }

        if (controllerPackage == null || controllerPackage.isBlank()) {
            throw new ServletException("Le paramètre controllerPackage doit être configuré dans web.xml");
        }

        String repositoryPackage = getServletContext().getInitParameter("repositoryPackage");

        String prefix = getServletContext().getInitParameter("viewPrefix");
        String suffix = getServletContext().getInitParameter("viewSuffix");
        viewResolver = new ViewResolver(prefix, suffix);

        responseResolvers = new ResponseResolverRegistry(List.of(
                new JsonResponseResolver(new ReflectiveJsonSerializer()),
                new ViewResponseResolver(viewResolver)
        ));

        try {
            initConnectionFactory();

            applicationContext = new ApplicationContext();
            if (connectionFactory != null) {
                applicationContext.registerSingleton(ConnectionFactory.class, connectionFactory);
            }
            if (repositoryPackage != null && !repositoryPackage.isBlank()) {
                applicationContext.scan(controllerPackage.trim(), repositoryPackage.trim());
            } else {
                applicationContext.scan(controllerPackage.trim());
            }
            LOGGER.info(() -> "Conteneur IoC initialisé : " + applicationContext.getBeanCount() + " beans");

            routeRegistry = new ControllerScanner(getClass().getClassLoader()).scan(controllerPackage.trim());
            LOGGER.info(() -> "Routes enregistrées : " + routeRegistry.size());
            for (RouteMapping route : routeRegistry.getAllRoutes()) {
                LOGGER.info(() -> route.toString());
            }
        } catch (IOException e) {
            throw new ServletException("Impossible de scanner les contrôleurs dans le package " + controllerPackage, e);
        }
    }

    private void initConnectionFactory() {
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbDriver = getServletContext().getInitParameter("dbDriver");

        if (dbUrl != null && !dbUrl.isBlank()
                && dbUser != null && !dbUser.isBlank()
                && dbDriver != null && !dbDriver.isBlank()) {
            DatabaseConfig dbConfig = new DatabaseConfig(dbUrl, dbUser,
                    dbPassword != null ? dbPassword : "", dbDriver);
            connectionFactory = new ConnectionFactory(dbConfig);
            getServletContext().setAttribute("connectionFactory", connectionFactory);
            LOGGER.info(() -> "ConnectionFactory initialisée pour : " + dbUrl);
        }
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = extractPath(req);
        String method = req.getMethod();

        var route = routeRegistry.find(method, path);
        if (route.isPresent()) {
            if ("GET".equals(method) && "/".equals(path)) {
                resp.setContentType(ViewResponseResolver.HTML_CONTENT_TYPE);
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

    public ResponseResolverRegistry getResponseResolvers() {
        return responseResolvers;
    }

    public ViewResolver getViewResolver() {
        return viewResolver;
    }

    private void invokeAndDispatch(HttpServletResponse resp, HttpServletRequest req, RouteMapping route) throws IOException {
        ResponseResolver resolver = responseResolvers.forRoute(route);

        try {
            Method handlerMethod = route.handlerMethod();
            Object controller = applicationContext.getBean(route.controllerClass());
            if (controller == null) {
                controller = route.controllerClass().getDeclaredConstructor().newInstance();
            }

            Model model = findModelParameter(handlerMethod);

            Object result = model != null
                    ? handlerMethod.invoke(controller, model)
                    : handlerMethod.invoke(controller);

            resolver.resolve(req, resp, route, model, result);
        } catch (Exception e) {
            Throwable cause = unwrap(e);
            LOGGER.log(Level.SEVERE,
                    "Erreur lors de l'invocation de " + route.controllerClass().getSimpleName()
                            + "#" + route.handlerMethod().getName() + " (" + route.key() + ")", cause);
            resolver.resolveError(req, resp, route, cause);
        }
    }

    /**
     * @return un modèle vide si l'action déclare un paramètre {@link Model},
     * {@code null} sinon
     */
    private Model findModelParameter(Method handlerMethod) {
        for (Parameter parameter : handlerMethod.getParameters()) {
            if (parameter.getType() == Model.class) {
                return new Model();
            }
        }
        return null;
    }

    private Throwable unwrap(Exception e) {
        if (e instanceof InvocationTargetException && e.getCause() != null) {
            return e.getCause();
        }
        return e;
    }

    private void writeUnknownUrl(HttpServletResponse resp, HttpServletRequest req, String method, String path) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType(ViewResponseResolver.HTML_CONTENT_TYPE);
        resp.getWriter().write("<p>URL inconnue : " + HtmlUtils.escape(method + " " + path) + "</p>");
        resp.getWriter().write("<p>Voici les URLs disponibles :</p><ul>");
        for (RouteMapping route : routeRegistry.getAllRoutes()) {
            resp.getWriter().write("<li>");
            if ("GET".equals(route.httpMethod())) {
                resp.getWriter().write("<a href=\"" + HtmlUtils.escape(buildUrl(req.getContextPath(), route.path())) + "\">");
                resp.getWriter().write(HtmlUtils.escape(route.httpMethod() + " " + route.path()));
                resp.getWriter().write("</a>");
            } else {
                resp.getWriter().write(HtmlUtils.escape(route.httpMethod() + " " + route.path()));
            }
            if (route.isJson()) {
                resp.getWriter().write(" <em>[JSON]</em>");
            }
            resp.getWriter().write("</li>");
        }
        resp.getWriter().write("</ul>");
        resp.getWriter().write("<p><a href=\"" + HtmlUtils.escape(req.getContextPath() + "/") + "\">Accueil</a></p>");
    }

    private void writeMethodNotAllowed(HttpServletResponse resp, String method, String path, List<RouteMapping> routes) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setContentType(ViewResponseResolver.HTML_CONTENT_TYPE);
        resp.getWriter().write("<p>405 — " + HtmlUtils.escape(method + " " + path) + " non autorisé</p><ul>");
        for (RouteMapping route : routes) {
            resp.getWriter().write("<li>" + HtmlUtils.escape(route.httpMethod() + " " + route.path()) + "</li>");
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
}
