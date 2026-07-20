package framework.view;

public class ViewResolver {

    private final String prefix;
    private final String suffix;

    public ViewResolver(String prefix, String suffix) {
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
    }

    public String resolve(String viewName) {
        return prefix + viewName + suffix;
    }
}
