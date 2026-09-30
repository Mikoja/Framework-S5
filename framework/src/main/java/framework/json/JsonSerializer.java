package framework.json;

/**
 * Produit une charge utile JSON à partir d'un objet Java.
 *
 * <p>Le dispatcher ne dépend que de cette interface : le remplacer par une
 * implémentation Jackson ou Gson ne demande aucune modification du front
 * controller.</p>
 */
public interface JsonSerializer {

    String toJson(Object value);
}
