package framework.response;

import framework.annotations.Json;
import framework.json.JsonMappingException;
import framework.json.JsonSerializer;
import framework.json.ReflectiveJsonSerializer;
import framework.model.Model;
import framework.routing.RouteMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Écrit la valeur de retour d'une action annotée {@link Json} directement dans
 * la réponse HTTP, sans passer par une vue.
 *
 * <table border="1">
 *     <caption>Règles de conversion</caption>
 *     <tr><th>Valeur de retour</th><th>Traitement</th></tr>
 *     <tr><td>{@code null}</td><td>le littéral JSON {@code null}</td></tr>
 *     <tr><td>{@link String} et {@code @Json(raw = true)}</td>
 *         <td>écrite telle quelle</td></tr>
 *     <tr><td>{@link String} commençant par <code>&#123;</code> ou <code>[</code></td>
 *         <td>considérée comme du JSON et écrite telle quelle</td></tr>
 *     <tr><td>autre {@link String}</td>
 *         <td>convertie en chaîne JSON, ex. {@code "L'API fonctionne"}</td></tr>
 *     <tr><td>Bean, {@link Iterable}, {@link Map}, tableau, nombre, booléen</td>
 *         <td>sérialisation complète</td></tr>
 * </table>
 */
public class JsonResponseResolver implements ResponseResolver {

    public static final String DEFAULT_CONTENT_TYPE = "application/json; charset=UTF-8";

    private static final Logger LOGGER = Logger.getLogger(JsonResponseResolver.class.getName());

    private final JsonSerializer serializer;
    private final String contentType;

    public JsonResponseResolver() {
        this(new ReflectiveJsonSerializer(), DEFAULT_CONTENT_TYPE);
    }

    public JsonResponseResolver(JsonSerializer serializer) {
        this(serializer, DEFAULT_CONTENT_TYPE);
    }

    public JsonResponseResolver(JsonSerializer serializer, String contentType) {
        this.serializer = serializer != null ? serializer : new ReflectiveJsonSerializer();
        this.contentType = contentType != null && !contentType.isBlank() ? contentType : DEFAULT_CONTENT_TYPE;
    }

    @Override
    public boolean supports(RouteMapping route) {
        return route != null && route.isJson();
    }

    @Override
    public void resolve(HttpServletRequest request,
                        HttpServletResponse response,
                        RouteMapping route,
                        Model model,
                        Object result) throws IOException {
        response.setContentType(contentType);
        response.getWriter().write(convert(result, route.json()));
    }

    @Override
    public void resolveError(HttpServletRequest request,
                             HttpServletResponse response,
                             RouteMapping route,
                             Throwable error) throws IOException {
        LOGGER.log(Level.SEVERE, "Erreur sur la route JSON " + route.key(), error);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", "Erreur interne du serveur");
        payload.put("message", messageOf(error));
        payload.put("path", route.path());

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(contentType);
        response.getWriter().write(serializer.toJson(payload));
    }

    private String convert(Object result, Json json) {
        if (result == null) {
            return "null";
        }

        if (result instanceof String text) {
            if (isRaw(json) || looksLikeJson(text)) {
                return text;
            }
            return serializer.toJson(text);
        }

        if (isRaw(json)) {
            LOGGER.warning(() -> "@Json(raw = true) : seules les valeurs de type String sont "
                    + "écrites telles quelles, la valeur de type " + result.getClass().getSimpleName()
                    + " est donc sérialisée par le framework");
        }

        try {
            return serializer.toJson(result);
        } catch (JsonMappingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new JsonMappingException("Conversion en JSON impossible pour "
                    + result.getClass().getName(), e);
        }
    }

    private boolean isRaw(Json json) {
        return json != null && json.raw();
    }

    /** Une chaîne déjà bien formée est reconnue à son premier caractère utile. */
    private boolean looksLikeJson(String text) {
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }
            return current == '{' || current == '[';
        }
        return false;
    }

    private String messageOf(Throwable error) {
        if (error == null) {
            return "Erreur inconnue";
        }
        return error.getMessage() != null ? error.getMessage() : error.getClass().getName();
    }
}
