package framework.view;

import framework.routing.RouteMapping;
import framework.routing.RouteRegistry;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public final class ControllerListingRenderer {

    private ControllerListingRenderer() {
    }

    public static void render(Writer writer, RouteRegistry routeRegistry, String contextPath) throws IOException {
        Map<Class<?>, List<RouteMapping>> controllers = routeRegistry.getRoutesByController();

        writer.write("<h1>Contrôleurs</h1>");
        writer.write("<p>" + controllers.size() + " contrôleur(s), " + routeRegistry.size() + " route(s)</p>");

        for (Map.Entry<Class<?>, List<RouteMapping>> entry : controllers.entrySet()) {
            writer.write("<h3>" + escapeHtml(entry.getKey().getSimpleName()) + "</h3><ul>");

            for (RouteMapping route : entry.getValue()) {
                writer.write("<li>");
                if ("GET".equals(route.httpMethod())) {
                    writer.write("<a href=\"" + escapeHtml(buildUrl(contextPath, route.path())) + "\">");
                    writer.write(escapeHtml(route.httpMethod() + " " + route.path()));
                    writer.write("</a>");
                } else {
                    writer.write(escapeHtml(route.httpMethod() + " " + route.path()));
                }
                writer.write("</li>");
            }

            writer.write("</ul>");
        }
    }

    private static String buildUrl(String contextPath, String path) {
        String base = contextPath == null ? "" : contextPath;
        if ("/".equals(path)) {
            return base.isEmpty() ? "/" : base + "/";
        }
        return base + path;
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
