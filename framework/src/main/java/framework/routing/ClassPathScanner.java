package framework.routing;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassPathScanner {

    private ClassPathScanner() {
    }

    public static List<Class<?>> findClasses(String packageName, ClassLoader classLoader) throws IOException {
        String packagePath = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        List<Class<?>> classes = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                scanDirectory(new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8)), packageName, classes, classLoader);
            } else if ("jar".equals(resource.getProtocol())) {
                scanJar(resource, packagePath, classes, classLoader);
            }
        }

        return classes;
    }

    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes, ClassLoader classLoader) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                loadClass(className, classLoader, classes);
            }
        }
    }

    private static void scanJar(URL resource, String packagePath, List<Class<?>> classes, ClassLoader classLoader) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entryName.contains("$")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    loadClass(className, classLoader, classes);
                }
            }
        }
    }

    private static void loadClass(String className, ClassLoader classLoader, List<Class<?>> classes) {
        try {
            classes.add(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // Ignore classes that cannot be loaded from the current classpath.
        }
    }
}
