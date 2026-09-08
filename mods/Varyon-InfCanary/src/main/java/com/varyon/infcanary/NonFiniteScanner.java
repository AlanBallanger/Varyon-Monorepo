package com.varyon.infcanary;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflectively walks an arbitrary object graph looking for a {@code float}/{@code double} field
 * (or array element, or {@link Number} inside a collection/map) whose value is NaN or infinite.
 *
 * <p>Kept deliberately conservative: bounded depth, bounded node count, cycle-safe, and it never
 * descends into JDK containers by field (only by their public iteration), so it will not blow the
 * stack on a packet that references large server state.
 */
final class NonFiniteScanner {

    static final class Hit {
        final String path;
        final double value;
        final String declaringType;

        Hit(String path, double value, String declaringType) {
            this.path = path;
            this.value = value;
            this.declaringType = declaringType;
        }
    }

    private static final int MAX_DEPTH = 12;
    private static final int MAX_NODES = 20_000;

    /** Per-class cached instance fields (including inherited), already {@code setAccessible(true)}. */
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private NonFiniteScanner() {}

    static Hit scan(Object root) {
        if (root == null) {
            return null;
        }
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(root, root.getClass().getSimpleName(), 0));
        int nodes = 0;

        while (!queue.isEmpty()) {
            if (++nodes > MAX_NODES) {
                return null;
            }
            Node node = queue.poll();
            Object obj = node.value;
            if (obj == null || node.depth > MAX_DEPTH) {
                continue;
            }
            Class<?> cls = obj.getClass();

            if (cls.isPrimitive()) {
                continue;
            }
            if (obj instanceof Float f) {
                if (!Float.isFinite(f)) {
                    return new Hit(node.path, f, "Float");
                }
                continue;
            }
            if (obj instanceof Double d) {
                if (!Double.isFinite(d)) {
                    return new Hit(node.path, d, "Double");
                }
                continue;
            }
            if (obj instanceof Number || obj instanceof CharSequence || obj instanceof Boolean
                    || obj instanceof Character || obj instanceof Enum<?>) {
                continue;
            }
            if (!seen.isEmpty() && seen.put(obj, Boolean.TRUE) != null) {
                continue;
            } else if (seen.isEmpty()) {
                seen.put(obj, Boolean.TRUE);
            }

            if (cls.isArray()) {
                Class<?> comp = cls.getComponentType();
                int len = Array.getLength(obj);
                if (comp == float.class) {
                    for (int i = 0; i < len; i++) {
                        float v = Array.getFloat(obj, i);
                        if (!Float.isFinite(v)) {
                            return new Hit(node.path + "[" + i + "]", v, "float[]");
                        }
                    }
                } else if (comp == double.class) {
                    for (int i = 0; i < len; i++) {
                        double v = Array.getDouble(obj, i);
                        if (!Double.isFinite(v)) {
                            return new Hit(node.path + "[" + i + "]", v, "double[]");
                        }
                    }
                } else if (!comp.isPrimitive()) {
                    for (int i = 0; i < len && i < 4096; i++) {
                        Object el = Array.get(obj, i);
                        if (el != null) {
                            queue.add(new Node(el, node.path + "[" + i + "]", node.depth + 1));
                        }
                    }
                }
                continue;
            }

            if (obj instanceof Collection<?> coll) {
                int i = 0;
                for (Object el : coll) {
                    if (i++ > 4096) {
                        break;
                    }
                    if (el != null) {
                        queue.add(new Node(el, node.path + ".<elem>", node.depth + 1));
                    }
                }
                continue;
            }
            if (obj instanceof Map<?, ?> map) {
                int i = 0;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (i++ > 4096) {
                        break;
                    }
                    if (e.getValue() != null) {
                        queue.add(new Node(e.getValue(), node.path + ".<val>", node.depth + 1));
                    }
                }
                continue;
            }

            // Only descend into protocol / math / mod classes. Skip the JDK and server-core internals
            // so we don't wander into unrelated live state that a packet might transitively hold.
            String name = cls.getName();
            if (name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")
                    || name.startsWith("io.netty.") || name.startsWith("it.unimi.")
                    || name.startsWith("com.google.")) {
                continue;
            }

            for (Field f : fieldsOf(cls)) {
                Object child;
                try {
                    child = f.get(obj);
                } catch (Throwable t) {
                    continue;
                }
                if (child == null) {
                    continue;
                }
                Class<?> ft = f.getType();
                String childPath = node.path + "." + f.getName();
                if (ft == float.class) {
                    float v = (Float) child;
                    if (!Float.isFinite(v)) {
                        return new Hit(childPath, v, cls.getSimpleName());
                    }
                } else if (ft == double.class) {
                    double v = (Double) child;
                    if (!Double.isFinite(v)) {
                        return new Hit(childPath, v, cls.getSimpleName());
                    }
                } else if (!ft.isPrimitive()) {
                    queue.add(new Node(child, childPath, node.depth + 1));
                }
            }
        }
        return null;
    }

    private static Field[] fieldsOf(Class<?> cls) {
        return FIELD_CACHE.computeIfAbsent(cls, c -> {
            java.util.ArrayList<Field> out = new java.util.ArrayList<>();
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        out.add(f);
                    } catch (Throwable ignored) {
                        // inaccessible (module boundary) — skip
                    }
                }
            }
            return out.toArray(new Field[0]);
        });
    }

    private static final class Node {
        final Object value;
        final String path;
        final int depth;

        Node(Object value, String path, int depth) {
            this.value = value;
            this.path = path;
            this.depth = depth;
        }
    }
}
