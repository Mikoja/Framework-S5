package framework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private String viewName;
    private final Map<String, Object> model;

    public ModelAndView(String viewName) {
        this.viewName = viewName;
        this.model = new HashMap<>();
    }

    public ModelAndView(String viewName, Map<String, Object> model) {
        this.viewName = viewName;
        this.model = model != null ? model : new HashMap<>();
    }

    public void setAttribute(String name, Object value) {
        model.put(name, value);
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }
}
