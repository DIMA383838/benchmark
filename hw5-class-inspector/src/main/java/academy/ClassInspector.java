package academy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class ClassInspector {

    private static final int MAX_RECURSION_DEPTH = 3;
    private static final Random RANDOM = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String inspect(Class<?> clazz, String format) {
        try {
            if ("TEXT".equalsIgnoreCase(format)) {
                return formatAsText(clazz);
            } else if ("JSON".equalsIgnoreCase(format)) {
                return formatAsJson(clazz);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to inspect class", e);
        }
    }

    private static String formatAsText(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();

        sb.append("Class: ").append(clazz.getSimpleName()).append("\n");

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            sb.append("Superclass: ").append(superclass.getSimpleName()).append("\n");
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            sb.append("Interfaces:\n");
            for (Class<?> iface : interfaces) {
                sb.append("  - ").append(iface.getSimpleName()).append("\n");
            }
        }

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            sb.append("Fields:\n");
            for (Field field : fields) {
                String access = Modifier.toString(field.getModifiers());
                sb.append("  - ")
                        .append(access)
                        .append(" ")
                        .append(field.getName())
                        .append(" (")
                        .append(field.getType().getSimpleName())
                        .append(")\n");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            sb.append("Methods:\n");
            for (Method method : methods) {
                String access = Modifier.toString(method.getModifiers());
                sb.append("  - ")
                        .append(access)
                        .append(" ")
                        .append(method.getName())
                        .append("(");

                Parameter[] params = method.getParameters();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params[i].getType().getSimpleName());
                }
                sb.append(") : ").append(method.getReturnType().getSimpleName()).append("\n");
            }
        }

        Annotation[] annotations = clazz.getAnnotations();
        if (annotations.length > 0) {
            sb.append("Annotations:\n");
            for (Annotation annotation : annotations) {
                sb.append("  - @")
                        .append(annotation.annotationType().getSimpleName())
                        .append("\n");
            }
        }

        sb.append("Hierarchy:\n").append(formatHierarchyText(clazz));

        return sb.toString();
    }

    private static String formatHierarchyText(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && !current.equals(Object.class)) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }

        Collections.reverse(hierarchy);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hierarchy.size(); i++) {
            // Добавляем отступы
            for (int j = 0; j < i; j++) {
                if (j == i - 1) {
                    sb.append("    ");
                } else {
                    sb.append("  ");
                }
            }

            if (i > 0) {

                for (int j = 0; j < i - 1; j++) {
                    sb.append("  ");
                }
                sb.append("└── ");
            }

            sb.append(hierarchy.get(i).getSimpleName());

            if (i < hierarchy.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatAsJson(Class<?> clazz) {
        ObjectNode root = MAPPER.createObjectNode();

        root.put("class", clazz.getSimpleName());

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            root.put("superclass", superclass.getSimpleName());
        }

        ArrayNode interfacesNode = root.putArray("interfaces");
        for (Class<?> iface : clazz.getInterfaces()) {
            interfacesNode.add(iface.getSimpleName());
        }

        ArrayNode fieldsNode = root.putArray("fields");
        for (Field field : clazz.getDeclaredFields()) {
            ObjectNode fieldNode = MAPPER.createObjectNode();
            fieldNode.put("access", Modifier.toString(field.getModifiers()));
            fieldNode.put("name", field.getName());
            fieldNode.put("type", field.getType().getSimpleName());
            fieldsNode.add(fieldNode);
        }

        ArrayNode methodsNode = root.putArray("methods");
        for (Method method : clazz.getDeclaredMethods()) {
            ObjectNode methodNode = MAPPER.createObjectNode();
            methodNode.put("access", Modifier.toString(method.getModifiers()));
            methodNode.put("name", method.getName());

            ArrayNode paramsNode = methodNode.putArray("params");
            for (Parameter param : method.getParameters()) {
                paramsNode.add(param.getType().getSimpleName());
            }

            methodNode.put("returnType", method.getReturnType().getSimpleName());
            methodsNode.add(methodNode);
        }

        ArrayNode annotationsNode = root.putArray("annotations");
        for (Annotation annotation : clazz.getAnnotations()) {
            annotationsNode.add(annotation.annotationType().getSimpleName());
        }

        root.set("hierarchy", buildHierarchyJsonFromRoot(clazz));

        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON", e);
        }
    }

    private static ObjectNode buildHierarchyJsonFromRoot(Class<?> clazz) {

        Class<?> root = clazz;
        while (root.getSuperclass() != null && !root.getSuperclass().equals(Object.class)) {
            root = root.getSuperclass();
        }

        return buildHierarchyJsonRecursive(root);
    }

    private static ObjectNode buildHierarchyJsonRecursive(Class<?> clazz) {
        ObjectNode node = MAPPER.createObjectNode();

        List<Class<?>> subclasses = getDirectSubclasses(clazz);

        for (Class<?> subclass : subclasses) {
            node.set(subclass.getSimpleName(), buildHierarchyJsonRecursive(subclass));
        }

        return node;
    }

    private static List<Class<?>> getDirectSubclasses(Class<?> clazz) {
        List<Class<?>> subclasses = new ArrayList<>();

        try {
            if (clazz.isSealed()) {
                Class<?>[] permitted = clazz.getPermittedSubclasses();
                if (permitted != null) {
                    subclasses.addAll(Arrays.asList(permitted));
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: sealed classes not supported in this Java version");
        }

        return subclasses;
    }

    public static <T> T create(Class<T> clazz) {
        return create(clazz, 0);
    }

    @SuppressWarnings("unchecked")
    private static <T> T create(Class<T> clazz, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return null;
        }

        try {

            if (clazz.isSealed() && clazz.getPermittedSubclasses() != null) {
                Class<?>[] permitted = clazz.getPermittedSubclasses();
                if (permitted.length > 0) {

                    Class<?> subclass = permitted[RANDOM.nextInt(permitted.length)];
                    return (T) create(subclass, depth + 1);
                }
            }

            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Constructor<?> constructor = Arrays.stream(constructors)
                    .min(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow(() -> new RuntimeException("No constructor found"));

            constructor.setAccessible(true);

            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                args[i] = createValue(paramTypes[i], depth + 1);
            }

            T instance = (T) constructor.newInstance(args);

            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    Object value = createValue(field.getType(), depth + 1);
                    field.set(instance, value);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private static Object createValue(Class<?> type, int depth) {
        if (type == String.class) {
            return "random_" + RANDOM.nextInt(1000);
        } else if (type == int.class || type == Integer.class) {
            return RANDOM.nextInt(100);
        } else if (type == long.class || type == Long.class) {
            return RANDOM.nextLong();
        } else if (type == double.class || type == Double.class) {
            return RANDOM.nextDouble();
        } else if (type == float.class || type == Float.class) {
            return RANDOM.nextFloat();
        } else if (type == boolean.class || type == Boolean.class) {
            return RANDOM.nextBoolean();
        } else if (type == byte.class || type == Byte.class) {
            return (byte) RANDOM.nextInt(Byte.MAX_VALUE);
        } else if (type == short.class || type == Short.class) {
            return (short) RANDOM.nextInt(Short.MAX_VALUE);
        } else if (type == char.class || type == Character.class) {
            return (char) ('a' + RANDOM.nextInt(26));
        } else if (type == Date.class) {
            return new Date(System.currentTimeMillis() - RANDOM.nextInt(1000000));
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            Object array = Array.newInstance(componentType, 3);
            for (int i = 0; i < 3; i++) {
                Array.set(array, i, createValue(componentType, depth + 1));
            }
            return array;
        } else if (List.class.isAssignableFrom(type)) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add(createValue(Object.class, depth + 1));
            }
            return list;
        } else if (Set.class.isAssignableFrom(type)) {
            Set<Object> set = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                set.add(createValue(Object.class, depth + 1));
            }
            return set;
        } else if (Map.class.isAssignableFrom(type)) {
            Map<Object, Object> map = new HashMap<>();
            for (int i = 0; i < 3; i++) {
                map.put("key" + i, createValue(Object.class, depth + 1));
            }
            return map;
        } else if (!type.isPrimitive() && !type.isInterface()) {
            return create(type, depth);
        }

        return null;
    }
}
