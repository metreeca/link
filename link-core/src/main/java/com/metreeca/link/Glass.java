/*
 * Copyright © 2023 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.link;


import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Character.isLowerCase;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.unmodifiableMap;
import static java.util.Map.entry;
import static java.util.stream.Collectors.joining;

/**
 * Class reflection utility ;-)
 */
final class Glass<T> {

    private static final Pattern FieldPattern=Pattern.compile("[A-Z][a-zA-Z0-9]*"); // !!! isJavaIdentifierPart()
    private static final Pattern GetterPattern=Pattern.compile("(is|get)("+FieldPattern+")");
    private static final Pattern SetterPattern=Pattern.compile("(set)("+FieldPattern+")");

    private static final Map<Class<?>, Object> DefaultValues=Map.ofEntries(
            entry(boolean.class, false),
            entry(char.class, '\u0000'),
            entry(byte.class, 0),
            entry(short.class, 0),
            entry(int.class, 0),
            entry(long.class, 0L),
            entry(float.class, 0.0f),
            entry(double.class, 0.0d)
    );

    private static final Map<Class<?>, Glass<?>> cache=new ConcurrentHashMap<>();


    @SuppressWarnings("unchecked")
    static <T> Glass<T> glass(final Class<?> clazz) {

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        return (Glass<T>)cache.computeIfAbsent(clazz, _clazz -> new Glass<T>(clazz));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static <T> T error(final String format, final Object... args) {
        throw new IllegalArgumentException(format(format, args));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Class<?> clazz;

    private final Map<String, Property> properties;


    private Glass(final Class<?> clazz) {
        this.clazz=clazz;
        this.properties=unmodifiableMap(properties(clazz, clazz, new LinkedHashMap<>()));
    }


    List<Class<?>> classes() {

        final List<Class<?>> classes=new ArrayList<>();

        for (Class<?> c=clazz; c != null && !c.equals(Object.class); c=c.getSuperclass()) {
            classes.add(c);
        }

        return classes;
    }

    Map<String, Property> properties() {
        return properties;
    }


    @SuppressWarnings("unchecked")
    T create() {
        try {

            return (T)clazz.getConstructor().newInstance();

        } catch ( final NoSuchMethodException
                        |IllegalAccessException
                        |InvocationTargetException
                        |InstantiationException e
        ) {

            throw new IllegalArgumentException(format(
                    "unable to create instance of <%s>", clazz
            ), e);

        }
    }

    T create(final T template) {

        final T copy=create();

        properties.values().forEach(property -> property.set(copy, property.get(template))); // !!! readable/writable

        return copy;

    }


    Object get(final T object, final String field) {
        return property(field).get(object);
    }

    void set(final T object, final String field, final Object value) {
        property(field).set(object, value);
    }


    private Property property(final String field) {
        return Optional.ofNullable(properties.get(field))
                .orElseThrow(() -> new IllegalArgumentException(format("unknown field <%s>", field)));
    }


    //// Bean Properties ///////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, Property> properties(
            final Class<?> initial, final Class<?> current, final Map<String, Property> properties
    ) {

        if ( current != null && current != Object.class ) {

            properties(initial, current.getSuperclass(), properties);  // scan parent class

            getters(current, properties);
            setters(current, properties);

            fields(current, properties); // backing fields to be scanned for annotations

        }

        // scan collection item types (must be performed wrt the target class without upward recursion)

        if ( initial.equals(current) ) {

            properties.entrySet().forEach(entry -> {

                final Property property=entry.getValue();

                final Type type=property.type();
                final Class<?> base=clazz(type);

                final Class<?> item=base != null && Collection.class.isAssignableFrom(base)
                        ? parameter(clazz, type)
                        : null;

                entry.setValue(property.merge(new Property(

                        null,

                        base,
                        item,

                        null,
                        null,

                        null

                )));

            });

        }

        return properties;
    }


    private void getters(final Class<?> clazz, final Map<String, Property> entries) {
        Arrays.stream(clazz.getDeclaredMethods())

                .filter(method -> !isStatic(method.getModifiers()))
                .filter(method -> isPublic(method.getModifiers()))

                .filter(method -> method.getReturnType() != Void.class)
                .filter(method -> method.getParameterCount() == 0)

                .forEachOrdered(method -> name(method, GetterPattern).ifPresent(name -> entries.merge(
                        name,
                        new Property(method.getGenericReturnType(), null, null, method, null, null),
                        Property::merge
                )));
    }

    private void setters(final Class<?> clazz, final Map<String, Property> entries) {
        Arrays.stream(clazz.getDeclaredMethods())

                .filter(method -> !isStatic(method.getModifiers()))
                .filter(method -> isPublic(method.getModifiers()))

                // no filtering on return type to support fluent setters

                .filter(method -> method.getParameterCount() == 1)

                .forEachOrdered(method -> name(method, SetterPattern).ifPresent(name -> entries.merge(
                        name,
                        new Property(method.getGenericParameterTypes()[0], null, null, null, method, null),
                        Property::merge
                )));
    }

    private void fields(final Class<?> clazz, final Map<String, Property> entries) {
        Arrays.stream(clazz.getDeclaredFields())

                .filter(method -> !isStatic(method.getModifiers()))
                .filter(field -> entries.containsKey(field.getName())) // only if linked to a public accessor

                .forEachOrdered(field -> entries.merge(
                        field.getName(),
                        new Property(field.getGenericType(), null, null, null, null, field),
                        Property::merge
                ));
    }


    private Optional<String> name(final Method method, final Pattern pattern) {
        return Optional.of(method.getName())
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .filter(matcher -> !matcher.group(1).equals("is")
                        || method.getReturnType().equals(Boolean.TYPE)
                        || method.getReturnType().equals(Boolean.class)
                )
                .map(matcher -> matcher.group(2))
                .map(name -> name.length() == 1
                        || name.length() >= 2 && isLowerCase(name.charAt(1))
                        || name.length() >= 3 && isLowerCase(name.charAt(2))
                        ? Character.toLowerCase(name.charAt(0))+name.substring(1)
                        : name
                );
    }


    //// Collection Item Type //////////////////////////////////////////////////////////////////////////////////////////

    private static Class<?> clazz(final Type type) {
        return type instanceof Class ? (Class<?>)type :
                type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType()
                        : null;
    }

    private static Class<?> parameter(final Class<?> clazz, final Type type) {
        if ( type instanceof Class ) {

            return (Class<?>)type;

        } else if ( type instanceof ParameterizedType ) {

            final Type argument=((ParameterizedType)type).getActualTypeArguments()[0];
            final Type binding=bindings(clazz, new HashMap<>()).getOrDefault(argument, argument);

            return binding instanceof Class ? (Class<?>)binding : null;

        } else {

            return null;

        }
    }

    private static Map<Type, Type> bindings(final Class<?> clazz, final Map<Type, Type> accumulator) {

        final Type superclass=(clazz == null) ? null : clazz.getGenericSuperclass();

        if ( superclass instanceof ParameterizedType ) {

            final TypeVariable<?>[] parameters=clazz.getSuperclass().getTypeParameters();
            final Type[] arguments=((ParameterizedType)superclass).getActualTypeArguments();

            for (int i=0; i < parameters.length; ++i) {
                accumulator.put(parameters[i], accumulator.getOrDefault(arguments[i], arguments[i]));
            }

            return bindings(clazz.getSuperclass(), accumulator);

        } else {

            return accumulator;

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class Property {

        private final Type type;

        private final Class<?> base;
        private final Class<?> item;

        private final Method getter;
        private final Method setter;

        private final Field field;


        private Property(

                final Type type,

                final Class<?> base,
                final Class<?> item,

                final Method getter,
                final Method setter,

                final Field field

        ) {

            this.type=type;

            this.base=base;
            this.item=item;

            this.getter=getter;
            this.setter=setter;

            this.field=field;

        }


        Property merge(final Property property) {

            return new Property(

                    type(property),

                    field(property, "base", p -> p.base),
                    field(property, "item", p -> p.item),

                    getter(property),
                    setter(property),

                    field(property, "field", p -> p.field)

            );
        }


        private Type type(final Property property) {

            final Type x=type;
            final Type y=property.type;

            return x == null ? y
                    : y == null ? x
                    : x instanceof Class && y instanceof Class && ((Class<?>)x).isAssignableFrom(((Class<?>)y)) ? y
                    : x instanceof Class && y instanceof Class && ((Class<?>)y).isAssignableFrom(((Class<?>)x)) ? x
                    : x.equals(y) ? x
                    : error(property, "type");
        }

        private Method getter(final Property property) {

            final Method x=getter;
            final Method y=property.getter;

            return x == null ? y
                    : y == null ? x
                    : x.getReturnType().isAssignableFrom(y.getReturnType()) ? y
                    : y.getReturnType().isAssignableFrom(x.getReturnType()) ? x
                    : error(property, "getter");
        }

        private Method setter(final Property property) {

            final Method x=setter;
            final Method y=property.setter;

            return x == null ? y
                    : y == null ? x
                    : x.getParameterTypes()[0].isAssignableFrom(y.getParameterTypes()[0]) ? x
                    : y.getParameterTypes()[0].isAssignableFrom(x.getParameterTypes()[0]) ? y
                    : error(property, "setter");
        }


        private <T> T field(final Property property, final String kind, final Function<Property, T> extractor) {

            final T x=extractor.apply(this);
            final T y=extractor.apply(property);

            return x == null ? y
                    : y == null || y.equals(x) ? x
                    : error(property, kind);
        }

        private <T> T error(final Property property, final String field) {
            throw new IllegalArgumentException(format(
                    "conflicting bean property <%s> %s / %s", field, this, property
            ));
        }


        <T extends Annotation> Optional<T> annotation(final Class<T> clazz) {
            return Stream.of(getter, setter, field)

                    .filter(Objects::nonNull)
                    .map(object -> object.getAnnotation(clazz))
                    .filter(Objects::nonNull)

                    .distinct()

                    .reduce((x, y) -> {

                        throw new IllegalArgumentException(format("conflicting annotations <%s> / <%s>", x, y));

                    });
        }


        Type type() { return type; }


        Class<?> base() { return base; }

        Optional<Class<?>> item() { return Optional.ofNullable(item); }


        @SuppressWarnings("unchecked") <T> T get(final Object object) {

            if ( getter == null ) {
                throw new IllegalStateException("write-only property");
            }

            try {

                return (T)getter.invoke(object);

            } catch ( final IllegalAccessException|InvocationTargetException e ) {

                throw new IllegalArgumentException(format("unable to invoke <%s>", getter), e);

            }

        }

        void set(final Object object, final Object value) {

            if ( setter == null ) {
                throw new IllegalStateException("read-only property");
            }

            final Class<?> expected=setter.getParameterTypes()[0];
            final Class<?> actual=value != null ? value.getClass() : Object.class;

            if ( Stash.class.isAssignableFrom(actual) && !Collection.class.isAssignableFrom(expected) ) {
                throw new IllegalArgumentException("unexpected query outside collection");
            }

            try {

                // !!! Sorted/NavigableSet
                // !!! QueryCollection

                if ( Set.class.equals(expected) && !Set.class.isAssignableFrom(actual) ) {

                    setter.invoke(object, value instanceof Collection
                            ? new LinkedHashSet<>((Collection<?>)value)
                            : value
                    );

                } else if ( List.class.equals(expected) && !List.class.isAssignableFrom(actual) ) {

                    setter.invoke(object, value instanceof Collection
                            ? new ArrayList<>((Collection<?>)value)
                            : value
                    );

                } else {

                    setter.invoke(object, value != null ? value : DefaultValues.get(base));

                }

            } catch ( final IllegalAccessException|InvocationTargetException|IllegalArgumentException e ) {

                throw new IllegalArgumentException(format("unable to invoke <%s>", setter), e);

            }
        }


        @Override public String toString() {
            return Stream

                    .of(
                            entry("getter", getter == null ? "-" : getter.toGenericString()),
                            entry("setter", setter == null ? "-" : setter.toGenericString()),
                            entry("field", field == null ? "-" : field.toGenericString())
                    )

                    .map(entry -> format("%s:\t%s", entry.getKey(), entry.getValue()))
                    .collect(joining("\n\t", "{\n\t", "\n}"));
        }

    }

}
