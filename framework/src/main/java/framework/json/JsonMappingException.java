package framework.json;

/**
 * Levée lorsqu'un objet ne peut pas être converti en JSON : getter inaccessible,
 * cycle de références, valeur non représentable, etc.
 */
public class JsonMappingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonMappingException(String message) {
        super(message);
    }

    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
