package framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class FrameworkProperties {

    private final Properties properties;

    private FrameworkProperties(Properties properties) {
        this.properties = properties;
    }

    public static FrameworkProperties load(String location) throws IOException {
        Properties properties = new Properties();
        InputStream in = FrameworkProperties.class.getClassLoader().getResourceAsStream(location);
        if (in == null) {
            Path path = Path.of(location);
            if (Files.isRegularFile(path)) {
                in = Files.newInputStream(path);
            }
        }
        if (in == null) {
            throw new IOException("Fichier de configuration introuvable : " + location);
        }
        try (InputStream stream = in) {
            properties.load(stream);
        }
        return new FrameworkProperties(properties);
    }

    public String get(String key) {
        return expand(properties.getProperty(key));
    }

    public String get(String key, String defaultValue) {
        return expand(properties.getProperty(key, defaultValue));
    }

    private String expand(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
