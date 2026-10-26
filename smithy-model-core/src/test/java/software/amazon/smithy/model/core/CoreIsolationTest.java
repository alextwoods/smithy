/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards that the smithy-model-core and smithy-utils-core jars work on their own.
 *
 * <p>The jars are loaded in a class loader whose parent is the platform class loader, so nothing
 * from smithy-model or smithy-utils is visible. Every class must load, link and initialize, except
 * the load-only passenger shape classes, which are in the core jar only so that reflection on
 * {@code Trait} and {@code ShapeId} works. Every public class must also support the reflection
 * calls that introspection tools make, and the main JSON, diff, error-message and serialization
 * paths must work.
 */
public class CoreIsolationTest {

    /** Classes that may be loaded but need smithy-model to initialize. */
    private static final Set<String> LOAD_ONLY_PASSENGERS = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            "software.amazon.smithy.model.shapes.EntityShape",
            "software.amazon.smithy.model.shapes.ServiceShape",
            "software.amazon.smithy.model.shapes.Shape")));

    private static final String NODE = "software.amazon.smithy.model.node.Node";
    private static final String TO_NODE = "software.amazon.smithy.model.node.ToNode";
    private static final String SHAPE_ID = "software.amazon.smithy.model.shapes.ShapeId";

    @Test
    public void coreJarsContainExactlyTheListedClasses() throws IOException {
        Set<String> listed = new TreeSet<>();
        for (Path list : pathsFromProperty("smithy.core.classLists")) {
            for (String line : Files.readAllLines(list, StandardCharsets.UTF_8)) {
                int comment = line.indexOf('#');
                String name = (comment >= 0 ? line.substring(0, comment) : line).trim();
                if (!name.isEmpty()) {
                    listed.add(name);
                }
            }
        }

        assertEquals(listed, new TreeSet<>(coreClassNames()));
    }

    @Test
    public void coreJarsDoNotSeeTheRestOfSmithy() throws IOException {
        try (URLClassLoader loader = isolatedLoader()) {
            for (String name : Arrays.asList(
                    "software.amazon.smithy.model.Model",
                    "software.amazon.smithy.model.shapes.MemberShape",
                    "software.amazon.smithy.model.validation.ValidationUtils",
                    "software.amazon.smithy.utils.StringUtils")) {
                assertThrows(ClassNotFoundException.class, () -> Class.forName(name, false, loader), name);
            }
        }
    }

    @Test
    public void everyCoreClassLinksExceptLoadOnlyPassengers() throws IOException {
        Map<String, String> failures = new TreeMap<>();
        List<String> names = coreClassNames();
        try (URLClassLoader loader = isolatedLoader()) {
            for (String name : names) {
                try {
                    Class.forName(name, true, loader);
                } catch (ClassNotFoundException | LinkageError e) {
                    failures.put(name, e.toString());
                }
            }
        }
        failures.keySet().removeAll(LOAD_ONLY_PASSENGERS);

        assertTrue(failures.isEmpty(), () -> "Core classes that fail to link or initialize: " + failures);
    }

    @Test
    public void loadOnlyPassengersCanBeLoaded() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            for (String name : LOAD_ONLY_PASSENGERS) {
                assertNotNull(Class.forName(name, false, loader));
            }
        }
    }

    @Test
    public void publicCoreClassesSupportReflection() throws Exception {
        Map<String, String> failures = new TreeMap<>();
        try (URLClassLoader loader = isolatedLoader()) {
            List<Class<?>> classes = new ArrayList<>();
            for (String name : coreClassNames()) {
                if (!LOAD_ONLY_PASSENGERS.contains(name)) {
                    Class<?> c = Class.forName(name, false, loader);
                    if (Modifier.isPublic(c.getModifiers())) {
                        classes.add(c);
                    }
                }
            }
            // A class outside Smithy that implements Trait, like an SDK trait class.
            Class<?> trait = Class.forName("software.amazon.smithy.model.traits.Trait", false, loader);
            classes.add(Proxy.newProxyInstance(loader, new Class<?>[] {trait}, (proxy, method, args) -> null)
                    .getClass());

            for (Class<?> c : classes) {
                try {
                    reflectOn(c);
                } catch (LinkageError | TypeNotPresentException e) {
                    failures.put(c.getName(), e.toString());
                }
            }
            Set<String> checked = classes.stream().map(Class::getName).collect(Collectors.toSet());
            assertTrue(checked.containsAll(Arrays.asList(trait.getName(), NODE, SHAPE_ID)), checked::toString);
        }

        assertTrue(failures.isEmpty(), () -> "Reflection failures with only the core jars: " + failures);
    }

    private static void reflectOn(Class<?> c) {
        for (Method method : c.getMethods()) {
            method.toGenericString();
        }
        for (Method method : c.getDeclaredMethods()) {
            method.toGenericString();
        }
        for (Constructor<?> constructor : c.getConstructors()) {
            constructor.toGenericString();
        }
        for (Constructor<?> constructor : c.getDeclaredConstructors()) {
            constructor.toGenericString();
        }
        c.getFields();
        c.getDeclaredFields();
        c.getClasses();
        c.getDeclaredClasses();
        c.getGenericInterfaces();
        c.getGenericSuperclass();
    }

    @Test
    public void parsesAndPrintsJson() throws Exception {
        String json = "{\"b\":[1,2.5,true,null,\"x\"],\"a\":{\"c\":\"d\"}}";
        try (URLClassLoader loader = isolatedLoader()) {
            Class<?> node = loader.loadClass(NODE);
            Object parsed = node.getMethod("parse", String.class).invoke(null, json);

            assertEquals(json, node.getMethod("printJson", node).invoke(null, parsed));
            String pretty = (String) node.getMethod("prettyPrintJson", node).invoke(null, parsed);
            assertTrue(pretty.contains("\n"), pretty);
            assertEquals(parsed, node.getMethod("parse", String.class).invoke(null, pretty));
            Object withComments = node.getMethod("parseJsonWithComments", String.class)
                    .invoke(null, "// comment\n" + json);
            assertEquals(parsed, withComments);
        }
    }

    @Test
    public void diffsUnequalNodes() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            Class<?> node = loader.loadClass(NODE);
            Class<?> toNode = loader.loadClass(TO_NODE);
            Method parse = node.getMethod("parse", String.class);
            Object actual = parse.invoke(null, "{\"a\":1}");
            Object expected = parse.invoke(null, "{\"a\":2}");

            assertEquals(Collections.singletonList("[/a]: Expected `2` but found `1`"),
                    node.getMethod("diff", toNode, toNode).invoke(null, actual, expected));
            Throwable error = invocationError(() -> node.getMethod("assertEquals", toNode, toNode)
                    .invoke(null, actual, expected));
            assertEquals("software.amazon.smithy.model.node.ExpectationNotMetException", error.getClass().getName());
        }
    }

    @Test
    public void formatsOneOfAndAdditionalPropertiesErrors() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            Class<?> node = loader.loadClass(NODE);
            Object string = node.getMethod("from", String.class).invoke(null, "foo");
            Throwable oneOf = invocationError(() -> string.getClass()
                    .getMethod("expectOneOf", Collection.class)
                    .invoke(string, Arrays.asList("xyz", "abc")));

            Object object = node.getMethod("parse", String.class).invoke(null, "{\"b\":1,\"a\":2}");
            Throwable additional = invocationError(() -> object.getClass()
                    .getMethod("expectNoAdditionalProperties", Collection.class)
                    .invoke(object, Collections.singletonList("c")));
            Object warned = object.getClass()
                    .getMethod("warnIfAdditionalProperties", Collection.class)
                    .invoke(object, Collections.singletonList("c"));

            assertEquals("Expected one of `abc`, `xyz`; got `foo`.", messageWithoutLocation(oneOf));
            assertEquals("Expected an object with possible properties of `c`, but found additional properties: "
                    + "`a`, `b`", messageWithoutLocation(additional));
            assertEquals(object, warned);
        }
    }

    @Test
    public void serializesShapeIds() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            Class<?> shapeId = loader.loadClass(SHAPE_ID);
            Object id = shapeId.getMethod("from", String.class).invoke(null, "ns.foo#Bar$baz");

            Object copy = deserialize(serialize(id), loader);
            assertEquals(id, copy);
            assertEquals(id.hashCode(), copy.hashCode());

            byte[] tampered = serialize(shapeId.getMethod("from", String.class).invoke(null, "a.b#Cd"));
            replaceOnce(tampered, "a.b#Cd", "a.b#C!");
            InvalidObjectException e = assertThrows(InvalidObjectException.class,
                    () -> deserialize(tampered, loader));
            assertEquals("software.amazon.smithy.model.shapes.ShapeIdSyntaxException",
                    e.getCause().getClass().getName());
        }
    }

    private static List<Path> pathsFromProperty(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "System property " + property + " must be set by the test task");
        return Arrays.stream(value.split(File.pathSeparator)).map(Paths::get).collect(Collectors.toList());
    }

    private static URLClassLoader isolatedLoader() throws IOException {
        List<URL> urls = new ArrayList<>();
        for (Path jar : pathsFromProperty("smithy.core.jars")) {
            urls.add(jar.toUri().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    }

    private static List<String> coreClassNames() throws IOException {
        List<String> names = new ArrayList<>();
        for (Path jar : pathsFromProperty("smithy.core.jars")) {
            try (JarFile file = new JarFile(jar.toFile())) {
                file.stream()
                        .map(entry -> entry.getName())
                        .filter(name -> name.endsWith(".class"))
                        .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
                        .forEach(names::add);
            }
        }
        assertFalse(names.isEmpty(), "No classes found in the core jars");
        return names;
    }

    private interface ReflectiveCall {
        void run() throws ReflectiveOperationException;
    }

    private static Throwable invocationError(ReflectiveCall call) {
        InvocationTargetException e = assertThrows(InvocationTargetException.class, call::run);
        return e.getCause();
    }

    private static String messageWithoutLocation(Throwable error) throws ReflectiveOperationException {
        return (String) error.getClass().getMethod("getMessageWithoutLocation").invoke(error);
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes, ClassLoader loader) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new LoaderObjectInputStream(new ByteArrayInputStream(bytes), loader)) {
            return in.readObject();
        }
    }

    private static void replaceOnce(byte[] bytes, String search, String replacement) {
        byte[] from = search.getBytes(StandardCharsets.UTF_8);
        byte[] to = replacement.getBytes(StandardCharsets.UTF_8);
        assertEquals(from.length, to.length);
        for (int i = 0; i <= bytes.length - from.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(bytes, i, i + from.length), from)) {
                System.arraycopy(to, 0, bytes, i, to.length);
                return;
            }
        }
        throw new AssertionError("Serialized form does not contain " + search);
    }

    private static final class LoaderObjectInputStream extends ObjectInputStream {
        private final ClassLoader loader;

        LoaderObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
            super(in);
            this.loader = loader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return Class.forName(desc.getName(), false, loader);
        }
    }
}
