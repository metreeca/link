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

import com.metreeca.link.Stash.Expression;
import com.metreeca.link.Stash.Transform;
import com.metreeca.link.jsonld.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.link.Glass.error;
import static com.metreeca.link.Glass.glass;
import static com.metreeca.link.Lingo.name;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public abstract class Shape {

    private static final String ReverseScheme="reverse:";

    private static final Map<Class<?>, Shape> cache=new ConcurrentHashMap<>();


    public static boolean forward(final String property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return !property.startsWith(ReverseScheme);
    }

    public static String reverse(final String property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return property.startsWith(ReverseScheme)
                ? property.substring(ReverseScheme.length())
                : ReverseScheme + property;
    }


    private static <T> Supplier<T> memoize(final Supplier<T> supplier) {
        return supplier instanceof Memoizer ? supplier : new Memoizer<>(supplier);
    }


    //// Declarative Factories /////////////////////////////////////////////////////////////////////////////////////////

    public static Shape clazz(final Class<?> clazz) {

        final Optional<Class<?>> value=Optional.ofNullable(clazz);

        return new Shape() {

            @Override public Optional<Class<?>> clazz() { return value; }

        };
    }


    public static Shape minCount(final Integer limit) {

        final Optional<Integer> value=Optional.ofNullable(limit).filter(v -> v != 0);

        if ( value.filter(v -> v < 0).isPresent() ) {
            throw new IllegalArgumentException("negative limit");
        }

        return new Shape() {

            @Override public Optional<Integer> minCount() { return value; }

        };
    }

    public static Shape maxCount(final Integer limit) {

        final Optional<Integer> value=Optional.ofNullable(limit).filter(v -> v != 0);

        if ( value.filter(v -> v < 0).isPresent() ) {
            throw new IllegalArgumentException("negative limit");
        }

        return new Shape() {

            @Override public Optional<Integer> maxCount() { return value; }

        };
    }


    public static Shape property(final String property, final Shape shape) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return property("", property, shape);
    }

    public static Shape property(final String field, final String property, final Shape shape) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return property(field, property, () -> shape);
    }

    public static Shape property(final String field, final String property, final Supplier<Shape> shape) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        final String _field=Optional.of(field)

                .filter(not(String::isEmpty))
                .or(() -> name(property))

                .orElseThrow(() -> new IllegalArgumentException(format(
                        "property <%s> is not an absolute IRI", property
                )));

        final Supplier<Shape> _shape=memoize(shape);

        return new Shape() {

            @Override public Optional<String> property(final String field) {

                if ( field == null ) {
                    throw new NullPointerException("null field");
                }

                return Optional.of(field).filter(_field::equals).map(f -> property);
            }

            @Override public Optional<Shape> shape(final String field) {

                if ( field == null ) {
                    throw new NullPointerException("null field");
                }

                return Optional.of(field).filter(_field::equals).map(f -> _shape.get());
            }

        };
    }


    public static Shape shape(final Shape... shapes) {

        if ( shapes == null ) {
            throw new NullPointerException("null shapes");
        }

        return shape(asList(shapes)); // ;( handle null values
    }

    public static Shape shape(final Collection<Shape> shapes) {

        if ( shapes == null ) {
            throw new NullPointerException("null shapes");
        }

        final boolean virtual=shapes.stream().anyMatch(Shape::virtual);

        final Optional<String> id=shapes.stream().flatMap(s -> s.id().stream()).reduce((x, y) ->
                x.equals(y) ? x : error("conflicting id definitions <%s> / <%s>", x, y)
        );

        final Set<String> types=shapes.stream().flatMap(Shape::types).collect(
                toCollection(LinkedHashSet::new)
        );

        final Optional<Class<?>> clazz=shapes.stream().flatMap(s -> s.clazz().stream()).reduce((x, y) ->
                y.isAssignableFrom(x) ? x : x.isAssignableFrom(y) ? y : error(
                        "conflicting clazz definitions <%s> / <%s>", x, y
                )
        );

        final Optional<Integer> minCount=shapes.stream().flatMap(s -> s.minCount().stream()).reduce((x, y) ->
                x >= y ? x : y
        );

        final Optional<Integer> maxCount=shapes.stream().flatMap(s -> s.maxCount().stream()).reduce((x, y) ->
                x <= y ? x : y
        );

        return new Shape() {

            @Override public boolean virtual() {
                return virtual;
            }


            @Override public Optional<String> id() { return id; }

            @Override public Stream<String> types() { return types.stream(); }


            @Override public Optional<Class<?>> clazz() { return clazz; }

            @Override public Optional<Integer> minCount() { return minCount; }

            @Override public Optional<Integer> maxCount() { return maxCount; }


            @Override public Optional<String> property(final String field) { // !!! memoize (requires field list)

                if ( field == null ) {
                    throw new NullPointerException("null field");
                }

                return shapes.stream().flatMap(shape -> shape.property(field).stream()).reduce((x, y) ->
                        x.equals(y) ? x : error("conflicting <%s> property definitions <%s> / <%s>", field, x, y)
                );

            }

            @Override public Optional<Shape> shape(final String field) { // !!! memoize (requires field list)

                if ( field == null ) {
                    throw new NullPointerException("null field");
                }

                return shapes.stream().flatMap(shape -> shape.shape(field).stream()).reduce(
                        Shape::shape
                );

            }

        };
    }


    //// Reflective Factory ////////////////////////////////////////////////////////////////////////////////////////////

    public static Shape shape(final Class<?> clazz) {

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        return cache.computeIfAbsent(clazz, _clazz -> {

            if ( _clazz.getName().startsWith("java.") || _clazz.getName().startsWith("javax.") ) {

                return clazz(_clazz);

            } else {

                final Glass<?> glass=glass(_clazz);
                final Lingo lingo=lingo(glass);

                final boolean virtual=virtual(glass);

                final Optional<String> id=id(glass);

                final Set<String> types=types(glass, lingo);
                final Set<String> links=links(glass, lingo);

                final Optional<Class<?>> klass=Optional.of(_clazz);

                final Map<String, String> properties=properties(glass, lingo);
                final Map<String, Supplier<Shape>> shapes=shapes(glass);

                return new Shape() {

                    @Override public boolean virtual() { return virtual; }


                    @Override public Optional<String> id() { return id; }

                    @Override public Stream<String> types() { return types.stream(); }


                    @Override public Optional<Class<?>> clazz() { return klass; }


                    @Override public Optional<String> property(final String field) {

                        if ( field == null ) {
                            throw new NullPointerException("null field");
                        }

                        return Optional.ofNullable(properties.get(field));
                    }

                    @Override public Optional<Shape> shape(final String field) {

                        if ( field == null ) {
                            throw new NullPointerException("null field");
                        }

                        return Optional.ofNullable(shapes.get(field)).map(Supplier::get);
                    }

                };

            }

        });
    }


    private static Lingo lingo(final Glass<?> glass) {

        final Lingo lingo=new Lingo();

        glass.classes().stream()

                .flatMap(c -> Stream.concat(

                        Optional.ofNullable(c.getAnnotation(Namespaces.class))
                                .map(Namespaces::value)
                                .stream()
                                .flatMap(Arrays::stream),

                        Optional.ofNullable(c.getAnnotation(Namespace.class))
                                .stream()

                ))

                .distinct()

                .forEach(namespace -> {

                    final String prefix=namespace.prefix();
                    final String value=namespace.value();

                    lingo.set(prefix, value);

                });

        return lingo;
    }


    private static boolean virtual(final Glass<?> glass) {
        return glass.classes().stream()
                .anyMatch(clazz -> clazz.getAnnotation(Virtual.class) != null);
    }

    private static Optional<String> id(final Glass<?> glass) {

        return glass.properties().entrySet().stream()

                .filter(entry -> entry.getValue().annotation(Id.class).isPresent())

                .peek(entry -> {

                    if ( !entry.getValue().base().equals(String.class) ) {
                        throw new IllegalArgumentException(format("@Id <%s> is not a string", entry));
                    }

                })

                .reduce((x, y) -> {

                    throw new IllegalArgumentException(format("multiple @Id definitions <%s> / <%s>", x, y));

                })

                .map(Map.Entry::getKey);

    }

    private static Set<String> types(final Glass<?> glass, final Lingo lingo) {
        return glass.classes().stream()
                .flatMap(c -> Optional
                        .ofNullable(c.getAnnotation(Type.class))
                        .map(Type::value)
                        .map(iri -> lingo.expand(iri, c.getSimpleName()))
                        .stream()
                )
                .collect(toCollection(LinkedHashSet::new));
    }

    private static Set<String> links(final Glass<?> glass, final Lingo lingo) {
        return glass.properties().entrySet().stream()

                .filter(entry -> {

                    final Glass.Property property=entry.getValue();

                    return property.annotation(Virtual.class).isPresent();

                })

                .map(entry -> { // !!! factor

                    final String field=entry.getKey();
                    final Glass.Property property=entry.getValue();

                    final String value=property.annotation(Property.class)
                            .map(Property::value)
                            .orElse(field);

                    final String iri=lingo.expand(value, field);

                    return property.annotation(Reverse.class).isPresent() ? reverse(iri) : iri;

                })

                .collect(toUnmodifiableSet());
    }


    private static Map<String, String> properties(final Glass<?> glass, final Lingo lingo) {
        return glass.properties().entrySet().stream().collect(toUnmodifiableMap(Map.Entry::getKey, entry -> { // !!! factor

            final String field=entry.getKey();
            final Glass.Property property=entry.getValue();

            final String value=property.annotation(Property.class)
                    .map(Property::value)
                    .orElse(field);

            final String iri=lingo.expand(value, field);

            return property.annotation(Reverse.class).isPresent() ? reverse(iri) : iri;

        }));
    }

    private static Map<String, Supplier<Shape>> shapes(final Glass<?> glass) {
        return glass.properties().entrySet().stream().collect(toUnmodifiableMap(Map.Entry::getKey, entry -> {

            final Glass.Property property=entry.getValue();

            return memoize(() -> shape(
                    shape(property.item().orElse(property.base())),
                    maxCount(property.item().isEmpty() ? 1 : null)
            ));

        }));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Shape() { }


    public boolean virtual() {
        return false;
    }


    public Optional<String> id() { // @id field mapping // !!! review
        return Optional.empty();
    }

    public Stream<String> types() { // from most to least specific
        return Stream.empty();
    }


    public Optional<Class<?>> clazz() { return Optional.empty(); }

    public Optional<Set<Object>> in() { return Optional.empty(); }


    public Optional<Object> minExclusive() { return Optional.empty(); }

    public Optional<Object> maxExclusive() { return Optional.empty(); }

    public Optional<Object> minInclusive() { return Optional.empty(); }

    public Optional<Object> maxInclusive() { return Optional.empty(); }


    public Optional<Integer> minLength() { return Optional.empty(); }

    public Optional<Integer> maxLength() { return Optional.empty(); }

    public Optional<String> pattern() { return Optional.empty(); }


    public Optional<Integer> minCount() { return Optional.empty(); }

    public Optional<Integer> maxCount() { return Optional.empty(); }


    public Optional<String> property(final String field) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        return Optional.empty();
    }

    public Optional<List<String>> properties(final List<String> fields) {

        if ( fields == null || fields.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null fields");
        }


        final List<String> path=new ArrayList<>();

        Shape next=this;

        for (final String field : fields) {

            final Optional<String> property=next.property(field);
            final Optional<Shape> shape=next.shape(field);

            if ( property.isEmpty() || shape.isEmpty() ) {

                return Optional.empty();

            } else {

                path.add(property.get());
                next=shape.get();

            }

        }

        return Optional.of(path);
    }


    public Optional<Shape> shape(final String field) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        return Optional.empty();
    }

    public Optional<Shape> shape(final Expression expression) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        return Optional.of(this)

                .flatMap(shape -> {

                    Optional<Shape> nested=Optional.of(shape);

                    for (final String step : expression.path()) {
                        nested=nested.flatMap(current -> current.shape(step));
                    }

                    return nested;

                })

                .flatMap(shape -> {

                    Optional<Shape> transformed=Optional.of(shape);

                    for (final Transform transform : expression.transforms()) {
                        transformed=transformed.flatMap(transform::apply);
                    }

                    return transformed;

                });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Memoizer<T> implements Supplier<T> {

        private T memo;

        private final Supplier<? extends T> supplier;


        private Memoizer(final Supplier<? extends T> supplier) { this.supplier=supplier; }


        @Override public T get() {
            return (memo != null) ? memo : (memo=supplier.get());
        }

    }

}
