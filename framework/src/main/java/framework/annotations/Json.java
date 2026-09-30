package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indique que la valeur de retour de la méthode doit être écrite telle quelle
 * dans la réponse HTTP au format JSON, au lieu d'être interprétée comme le nom
 * d'une vue à dispatcher.
 *
 * <p>Appliquée sur la classe, toutes ses méthodes d'action deviennent des
 * points d'entrée JSON.</p>
 *
 * <pre>{@code
 * @Get
 * @Json
 * public List<Mouvement> liste() {   // -> [ {...}, {...} ]
 *     return repository.findAll();
 * }
 *
 * @Get("/stats")
 * @Json(raw = true)
 * public String stats() {            // -> {"total":3}  (déjà du JSON)
 *     return "{\"total\":3}";
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Json {

    /**
     * La valeur de retour est déjà du JSON et ne doit pas être re-sérialisée.
     * N'a de sens que pour une valeur de type {@link String}, qui doit alors
     * contenir un document JSON valide.
     */
    boolean raw() default false;
}
