package framework.model;

import java.util.HashMap;
import java.util.Map;

public class Model {

    private final Map<String, Object> attributes = new HashMap<>();

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
