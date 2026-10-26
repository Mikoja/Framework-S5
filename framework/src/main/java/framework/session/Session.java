package framework.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private final String id;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private volatile boolean valid = true;

    public Session(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public <T> T getAttribute(String name, Class<T> type) {
        Object value = attributes.get(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public Map<String, Object> getAttributes() {
        return Map.copyOf(attributes);
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
        attributes.clear();
    }
}
