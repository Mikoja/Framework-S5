package framework.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sérialiseur JSON par réflexion, sans dépendance externe.
 *
 * <p>Un bean est rendu à partir de ses getters {@code getX()}/{@code isX()},
 * puis de ses champs publics non transitoires, en remontant la hiérarchie de
 * classes. Les {@link Map} deviennent des objets, les {@link Iterable} et les
 * tableaux deviennent des tableaux JSON.</p>
 *
 * <p>Les cycles de références sont signalés par une {@link JsonMappingException}
 * plutôt que tronqués silencieusement.</p>
 */
public class ReflectiveJsonSerializer implements JsonSerializer {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final Map<Class<?>, List<Property>> propertyCache = new ConcurrentHashMap<>();

    @Override
    public String toJson(Object value) {
        StringBuilder out = new StringBuilder(256);
        write(value, out, Collections.newSetFromMap(new IdentityHashMap<>()));
        return out.toString();
    }

    private void write(Object value, StringBuilder out, Set<Object> visited) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof String text) {
            writeString(text, out);
            return;
        }
        if (value instanceof Character character) {
            writeString(String.valueOf(character), out);
            return;
        }
        if (value instanceof Boolean bool) {
            out.append(bool.booleanValue());
            return;
        }
        if (value instanceof Enum<?> enumeration) {
            writeString(enumeration.name(), out);
            return;
        }
        if (value instanceof Number number) {
            writeNumber(number, out);
            return;
        }
        if (value instanceof Optional<?> optional) {
            write(optional.orElse(null), out, visited);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            writeMap(map, out, visited);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, out, visited);
            return;
        }
        if (value.getClass().isArray()) {
            writeArray(value, out, visited);
            return;
        }
        if (value instanceof Date date) {
            writeString(date.toInstant().toString(), out);
            return;
        }
        if (value instanceof TemporalAccessor temporal) {
            writeString(temporal.toString(), out);
            return;
        }
        if (value instanceof Class<?> type) {
            writeString(type.getName(), out);
            return;
        }
        writeBean(value, out, visited);
    }

    private void writeNumber(Number number, StringBuilder out) {
        if (number instanceof Double d && (d.isNaN() || d.isInfinite())) {
            out.append("null");
            return;
        }
        if (number instanceof Float f && (f.isNaN() || f.isInfinite())) {
            out.append("null");
            return;
        }
        out.append(number);
    }

    private void writeMap(Map<?, ?> map, StringBuilder out, Set<Object> visited) {
        enter(map, visited);
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                out.append(',');
            }
            writeString(String.valueOf(entry.getKey()), out);
            out.append(':');
            write(entry.getValue(), out, visited);
        }
        out.append('}');
        visited.remove(map);
    }

    private void writeIterable(Iterable<?> iterable, StringBuilder out, Set<Object> visited) {
        enter(iterable, visited);
        out.append('[');
        boolean first = true;
        for (Object item : iterable) {
            if (first) {
                first = false;
            } else {
                out.append(',');
            }
            write(item, out, visited);
        }
        out.append(']');
        visited.remove(iterable);
    }

    private void writeArray(Object array, StringBuilder out, Set<Object> visited) {
        enter(array, visited);
        int length = Array.getLength(array);
        out.append('[');
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                out.append(',');
            }
            write(Array.get(array, i), out, visited);
        }
        out.append(']');
        visited.remove(array);
    }

    private void writeBean(Object bean, StringBuilder out, Set<Object> visited) {
        Class<?> type = bean.getClass();

        // Les types valeur de la JDK (UUID, URI, Duration, Pattern, Locale...)
        // passent par leur toString() : exposer leurs getters produirait un
        // objet incompréhensible côté client.
        if (isJdkType(type) && hasCustomToString(type)) {
            writeString(String.valueOf(bean), out);
            return;
        }

        List<Property> properties = propertiesOf(type);
        if (properties.isEmpty()) {
            if (hasCustomToString(type)) {
                writeString(String.valueOf(bean), out);
            } else {
                out.append("{}");
            }
            return;
        }

        enter(bean, visited);
        out.append('{');
        boolean first = true;
        for (Property property : properties) {
            if (first) {
                first = false;
            } else {
                out.append(',');
            }
            writeString(property.name(), out);
            out.append(':');
            write(property.read(bean), out, visited);
        }
        out.append('}');
        visited.remove(bean);
    }

    private boolean isJdkType(Class<?> type) {
        Package typePackage = type.getPackage();
        if (typePackage == null) {
            return false;
        }
        String name = typePackage.getName();
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.");
    }

    private void enter(Object value, Set<Object> visited) {
        if (!visited.add(value)) {
            throw new JsonMappingException(
                    "Cycle de référence détecté sur " + value.getClass().getName()
                            + " : la sérialisation JSON ne peut pas le traiter");
        }
    }

    private void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20) {
                        out.append("\\u")
                                .append(HEX_DIGITS[(current >> 12) & 0xF])
                                .append(HEX_DIGITS[(current >> 8) & 0xF])
                                .append(HEX_DIGITS[(current >> 4) & 0xF])
                                .append(HEX_DIGITS[current & 0xF]);
                    } else {
                        out.append(current);
                    }
                }
            }
        }
        out.append('"');
    }

    private List<Property> propertiesOf(Class<?> type) {
        return propertyCache.computeIfAbsent(type, this::describeProperties);
    }

    private List<Property> describeProperties(Class<?> type) {
        Map<String, Property> properties = new LinkedHashMap<>();

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String name = propertyNameOf(method);
                if (name != null) {
                    properties.putIfAbsent(name, new Property(name, method, null));
                }
            }
        }

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (isExposed(field) && !properties.containsKey(field.getName())) {
                    properties.put(field.getName(), new Property(field.getName(), null, field));
                }
            }
        }

        return List.copyOf(properties.values());
    }

    private String propertyNameOf(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers) || method.isSynthetic() || method.isBridge()) {
            return null;
        }
        if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
            return null;
        }

        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    private boolean isExposed(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers)
                && !Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && !field.isSynthetic();
    }

    private String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private boolean hasCustomToString(Class<?> type) {
        try {
            return type.getMethod("toString").getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private record Property(String name, Method getter, Field field) {

        Object read(Object target) {
            try {
                return getter != null ? getter.invoke(target) : field.get(target);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new JsonMappingException(
                        "Impossible de lire la propriété '" + name + "' de " + target.getClass().getName(), cause);
            } catch (ReflectiveOperationException e) {
                throw new JsonMappingException(
                        "Impossible de lire la propriété '" + name + "' de " + target.getClass().getName(), e);
            }
        }
    }
}
