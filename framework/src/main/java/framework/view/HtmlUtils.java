package framework.view;

/**
 * Utilitaires d'échappement pour les fragments HTML générés par le framework.
 */
public final class HtmlUtils {

    private HtmlUtils() {
    }

    /**
     * @return la valeur échappée, sûre à insérer dans du HTML
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
