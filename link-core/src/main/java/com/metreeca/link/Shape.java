/*
 * Copyright © 2023-2024 Metreeca srl
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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
public abstract class Shape {

    //// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean derives(final IRI upper, final IRI lower) {
        return upper.equals(VALUE)
                || upper.equals(RESOURCE) && resource(lower)
                || upper.equals(LITERAL) && literal(lower);
    }


    private static boolean resource(final IRI type) {
        return type.equals(RESOURCE) || type.equals(BNODE) || type.equals(IRI);
    }

    private static boolean literal(final IRI type) {
        return type.equals(LITERAL) || !type.equals(VALUE) && !resource(type);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Shape virtual(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return virtual(asList(shapes));
    }

    public static Shape virtual(final Collection<Shape> shapes) {

        if ( shapes == null || shapes.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(shape(shapes), new Shape() {

            @Override public boolean virtual() { return true; }

        });
    }


    public static Shape composite(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return composite(asList(shapes));
    }

    public static Shape composite(final Collection<Shape> shapes) {

        if ( shapes == null || shapes.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(shape(shapes), new Shape() {

            @Override public boolean composite() { return true; }

        });
    }


    public static Shape datatype(final IRI datatype) {

        if ( datatype == null ) {
            throw new NullPointerException("null datatype");
        }

        final Optional<IRI> value=Optional.of(datatype)
                .filter(not(NIL::equals));

        return new Shape() {

            @Override public Optional<IRI> datatype() { return value; }

        };
    }


    public static Shape id() {
        return datatype(IRI);
    }

    public static Shape bool() {
        return datatype(XSD.BOOLEAN);
    }

    public static Shape string() {
        return datatype(XSD.STRING);
    }

    public static Shape string(final int length) {

        if ( length < 0 ) {
            throw new IllegalArgumentException(format("negative length <%d>", length));
        }

        return shape(string(), maxLength(length));
    }

    public static Shape integer() {
        return datatype(XSD.INTEGER);
    }

    public static Shape integer(final int lower) {
        return shape(integer(), minInclusive(Frame.literal(Frame.integer(lower))));
    }

    public static Shape integer(final int lower, final int upper) {

        if ( lower > upper ) {
            throw new IllegalArgumentException(format("inconsistent range [%d, %s]", lower, upper));
        }

        return shape(integer(),
                minInclusive(Frame.literal(Frame.integer(lower))),
                maxInclusive(Frame.literal(Frame.integer(upper)))
        );
    }

    public static Shape decimal() {
        return datatype(XSD.DECIMAL);
    }

    public static Shape decimal(final double lower) {
        return shape(decimal(), minInclusive(Frame.literal(Frame.decimal(lower))));
    }

    public static Shape decimal(final double lower, final double upper) {

        if ( lower > upper ) {
            throw new IllegalArgumentException(format("inconsistent range [%d, %s]", lower, upper));
        }

        return shape(decimal(),
                minInclusive(Frame.literal(Frame.decimal(lower))),
                maxInclusive(Frame.literal(Frame.decimal(upper)))
        );
    }

    public static Shape year() {
        return datatype(XSD.GYEAR);
    }

    public static Shape date() {
        return datatype(XSD.DATE);
    }

    public static Shape time() {
        return datatype(XSD.TIME);
    }

    public static Shape dateTime() {
        return datatype(XSD.DATETIME);
    }

    public static Shape instant() {
        return shape(dateTime(), pattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.\\d{3}Z"));
    }

    public static Shape duration() {
        return shape(datatype(XSD.DURATION));
    }

    public static Shape local() {
        return datatype(RDF.LANGSTRING);
    }


    public static Shape type(final IRI... types) {

        if ( types == null || Arrays.stream(types).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return type(List.of(types));
    }

    public static Shape type(final Collection<IRI> types) {

        if ( types == null || types.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        final Optional<Set<IRI>> value=Optional
                .of(types.stream()
                        .filter(not(NIL::equals))
                        .filter(not(RDFS.RESOURCE::equals))
                        .collect(toUnmodifiableSet())
                )
                .filter(not(Set::isEmpty));

        return new Shape() {

            @Override public Optional<Set<IRI>> type() { return value; }

        };
    }


    public static Shape minExclusive(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> value=Optional.of(limit)
                .filter(not(NIL::equals));

        return new Shape() {

            @Override public Optional<Value> minExclusive() { return value; }

        };

    }

    public static Shape maxExclusive(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> value=Optional.of(limit)
                .filter(not(NIL::equals));

        return new Shape() {

            @Override public Optional<Value> maxExclusive() { return value; }

        };

    }

    public static Shape minInclusive(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> value=Optional.of(limit)
                .filter(not(NIL::equals));

        return new Shape() {

            @Override public Optional<Value> minInclusive() { return value; }

        };

    }

    public static Shape maxInclusive(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> value=Optional.of(limit)
                .filter(not(NIL::equals));

        return new Shape() {

            @Override public Optional<Value> maxInclusive() { return value; }

        };

    }


    public static Shape minLength(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        final Optional<Integer> value=Optional.of(limit)
                .filter(v -> v != 0);

        return new Shape() {

            @Override public Optional<Integer> minLength() { return value; }

        };
    }

    public static Shape maxLength(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        final Optional<Integer> value=Optional.of(limit)
                .filter(v -> v != 0);

        return new Shape() {

            @Override public Optional<Integer> maxLength() { return value; }

        };
    }

    public static Shape pattern(final String pattern) {

        if ( pattern == null ) {
            throw new NullPointerException("null pattern");
        }

        final Optional<String> value=Optional.of(pattern)
                .filter(not(String::isBlank));

        return new Shape() {

            @Override public Optional<String> pattern() { return value; }

        };
    }


    public static Shape minCount(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        final Optional<Integer> value=Optional.of(limit)
                .filter(v -> v != 0);

        return new Shape() {

            @Override public Optional<Integer> minCount() { return value; }

        };
    }

    public static Shape maxCount(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        final Optional<Integer> value=Optional.of(limit)
                .filter(v -> v != 0);

        return new Shape() {

            @Override public Optional<Integer> maxCount() { return value; }

        };
    }


    public static Shape required(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(minCount(1), maxCount(1), shape(shapes));
    }

    public static Shape optional(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(maxCount(1), shape(shapes));
    }

    public static Shape repeatable(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(minCount(1), shape(shapes));
    }

    public static Shape multiple(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(shapes);
    }


    public static Shape in(final Value... values) {

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return in(List.of(values));
    }

    public static Shape in(final Collection<Value> values) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        final Optional<Set<Value>> value=Optional
                .of(values.stream()
                        .filter(not(NIL::equals))
                        .collect(toUnmodifiableSet())
                )
                .filter(not(Set::isEmpty));

        return new Shape() {

            @Override public Optional<Set<Value>> in() { return value; }

        };
    }


    public static Shape hasValue(final Value... values) {

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return hasValue(List.of(values));
    }

    public static Shape hasValue(final Collection<Value> values) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        final Optional<Set<Value>> value=Optional
                .of(values.stream()
                        .filter(not(NIL::equals))
                        .collect(toUnmodifiableSet())
                )
                .filter(not(Set::isEmpty));

        return new Shape() {

            @Override public Optional<Set<Value>> hasValue() { return value; }

        };
    }


    public static Shape property(final IRI predicate, final Shape shape) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return property(label(predicate), predicate, () -> shape);
    }

    public static Shape property(final IRI predicate, final Supplier<? extends Shape> shape) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return property(label(predicate), predicate, shape);
    }

    public static Shape property(final String label, final IRI predicate, final Shape shape) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return property(label, predicate, () -> shape);
    }

    public static Shape property(final String label, final IRI predicate, final Supplier<? extends Shape> shape) {

        if ( label == null ) {
            throw new NullPointerException("null alias");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }


        final Optional<IRI> datatype=forward(predicate)
                ? Optional.of(RESOURCE)
                : Optional.empty();

        final Supplier<Shape> supplier=new Cache<>(() -> {

            if ( predicate.equals(ID) ) {

                return shape(shape.get(), datatype(RESOURCE), maxCount(1));

            } else if ( predicate.equals(TYPE) ) {

                return shape(shape.get(), datatype(RESOURCE));

            } else {

                return shape.get();

            }

        });

        final Map<String, Entry<IRI, Supplier<Shape>>> properties=Map.of(label, Map.entry(predicate, supplier));
        final Map<IRI, Entry<String, Supplier<Shape>>> labels=Map.of(predicate, Map.entry(label, supplier));

        return new Shape() {

            @Override public Optional<IRI> datatype() { return datatype; }

            @Override public Map<String, Entry<IRI, Supplier<Shape>>> labels() { return properties; }

            @Override public Map<IRI, Entry<String, Supplier<Shape>>> predicates() { return labels; }

        };
    }


    public static Shape shape() {
        return new Shape() { };
    }

    public static Shape shape(final Shape... shapes) {

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(NIL, List.of(shapes));
    }

    public static Shape shape(final Collection<Shape> shapes) {

        if ( shapes == null || shapes.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(NIL, shapes);
    }

    public static Shape shape(final IRI target, final Shape... shapes) {

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        return shape(target, List.of(shapes));
    }

    public static Shape shape(final IRI target, final Collection<Shape> shapes) {

        if ( shapes == null || shapes.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        if ( target.equals(NIL) && shapes.isEmpty() ) {

            return shape();

        } else if ( target.equals(NIL) && shapes.size() == 1 ) {

            return shapes.iterator().next();

        } else {

            final boolean virtual=shapes.stream().anyMatch(s -> s.virtual());
            final boolean composite=shapes.stream().anyMatch(s -> s.composite());

            final Optional<IRI> clazz=Optional.of(target)
                    .filter(not(NIL::equals))
                    .filter(not(RDFS.RESOURCE::equals));


            final Optional<IRI> datatype=Stream
                    .concat(
                            clazz.isEmpty() ? Stream.empty() : Stream.of(RESOURCE),
                            shapes.stream().flatMap(s -> s.datatype().stream())
                    )
                    .reduce((x, y) -> x.equals(y) ? x
                            : derives(x, y) ? y
                            : derives(y, x) ? x
                            : error("conflicting <datatype> constraints <%s> / <%s>", x, y)
                    );

            final Optional<Set<IRI>> type=shapes.stream()
                    .flatMap(s -> s.type().stream())
                    .reduce((x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toUnmodifiableSet()));


            final Optional<Value> minExclusive=shapes.stream()
                    .flatMap(s -> s.minExclusive().stream())
                    .reduce((x, y) -> compare(x, y) <= 0 ? x : y);

            final Optional<Value> maxExclusive=shapes.stream()
                    .flatMap(s -> s.maxExclusive().stream())
                    .reduce((x, y) -> compare(x, y) >= 0 ? x : y
                    );

            final Optional<Value> minInclusive=shapes.stream()
                    .flatMap(s -> s.minInclusive().stream())
                    .reduce((x, y) -> compare(x, y) <= 0 ? x : y);

            final Optional<Value> maxInclusive=shapes.stream()
                    .flatMap(s -> s.maxInclusive().stream())
                    .reduce((x, y) -> compare(x, y) >= 0 ? x : y);


            final Optional<Integer> minLength=shapes.stream()
                    .flatMap(s -> s.minLength().stream())
                    .reduce((x, y) -> x >= y ? x : y);

            final Optional<Integer> maxLength=shapes.stream()
                    .flatMap(s -> s.maxLength().stream())
                    .reduce((x, y) -> x <= y ? x : y);

            final Optional<String> pattern=shapes.stream()
                    .flatMap(s -> s.pattern().stream())
                    .reduce((x, y) -> x.equals(y) ? x : error("conflicting <pattern> constraints <%s> / <%s>", x, y));


            final Optional<Integer> minCount=shapes.stream()
                    .flatMap(s -> s.minCount().stream())
                    .reduce((x, y) -> x >= y ? x : y);

            final Optional<Integer> maxCount=shapes.stream()
                    .flatMap(s -> s.maxCount().stream())
                    .reduce((x, y) -> x <= y ? x : y);

            final Optional<Set<Value>> in=shapes.stream()
                    .flatMap(s -> s.in().stream())
                    .reduce((x, y) -> {

                        final Set<Value> i=x.stream().filter(y::contains).collect(toUnmodifiableSet());

                        return i.isEmpty() ? error("conflicting <in> constraints <%s> / <%s>", x, y) : i;

                    });

            final Optional<Set<Value>> hasValue=shapes.stream()
                    .flatMap(s -> s.hasValue().stream())
                    .reduce((x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toUnmodifiableSet()));


            final Map<String, Entry<IRI, Supplier<Shape>>> properties=shapes.stream()
                    .flatMap(shape -> shape.labels().entrySet().stream())
                    .collect(toUnmodifiableMap(Entry::getKey, Entry::getValue, (x, y) ->
                            x.getKey().equals(y.getKey())
                                    ? Map.entry(x.getKey(), () -> shape(x.getValue().get(), y.getValue().get()))
                                    : error("conflicting property predicate <%s> / <%s>", x.getKey(), y.getKey())
                    ));

            final Map<IRI, Entry<String, Supplier<Shape>>> labels=shapes.stream()
                    .flatMap(shape -> shape.predicates().entrySet().stream())
                    .collect(toUnmodifiableMap(Entry::getKey, Entry::getValue, (x, y) ->
                            x.getKey().equals(y.getKey())
                                    ? Map.entry(x.getKey(), () -> shape(x.getValue().get(), y.getValue().get()))
                                    : error("conflicting property label <%s> / <%s>", x.getKey(), y.getKey())
                    ));


            return new Shape() {

                @Override public boolean virtual() {
                    return virtual;
                }

                @Override public boolean composite() {
                    return composite;
                }


                @Override public Optional<IRI> target() {
                    return clazz;
                }


                @Override public Optional<IRI> datatype() { return datatype; }

                @Override public Optional<Set<IRI>> type() { return type; }


                @Override public Optional<Value> minExclusive() { return minExclusive; }

                @Override public Optional<Value> maxExclusive() { return maxExclusive; }

                @Override public Optional<Value> minInclusive() { return minInclusive; }

                @Override public Optional<Value> maxInclusive() { return maxInclusive; }


                @Override public Optional<Integer> minLength() { return minLength; }

                @Override public Optional<Integer> maxLength() { return maxLength; }

                @Override public Optional<String> pattern() { return pattern; }


                @Override public Optional<Integer> minCount() { return minCount; }

                @Override public Optional<Integer> maxCount() { return maxCount; }


                @Override public Optional<Set<Value>> in() { return in; }

                @Override public Optional<Set<Value>> hasValue() { return hasValue; }


                @Override public Map<String, Entry<IRI, Supplier<Shape>>> labels() {
                    return properties;
                }

                @Override public Map<IRI, Entry<String, Supplier<Shape>>> predicates() {
                    return labels;
                }

            };
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String label(final IRI predicate) {
        return predicate.equals(ID) ? _ID
                : predicate.equals(TYPE) ? _TYPE
                : predicate.getLocalName();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Shape() { }


    public boolean virtual() { return false; }

    public boolean composite() { return false; }


    public Optional<IRI> target() { return Optional.empty(); }


    public Optional<IRI> datatype() { return Optional.empty(); }

    public Optional<Set<IRI>> type() { return Optional.empty(); }


    public Optional<Value> minExclusive() { return Optional.empty(); }

    public Optional<Value> maxExclusive() { return Optional.empty(); }

    public Optional<Value> minInclusive() { return Optional.empty(); }

    public Optional<Value> maxInclusive() { return Optional.empty(); }


    public Optional<Integer> minLength() { return Optional.empty(); }

    public Optional<Integer> maxLength() { return Optional.empty(); }

    public Optional<String> pattern() { return Optional.empty(); }


    public Optional<Integer> minCount() { return Optional.empty(); }

    public Optional<Integer> maxCount() { return Optional.empty(); }


    public Optional<Set<Value>> in() { return Optional.empty(); }

    public Optional<Set<Value>> hasValue() { return Optional.empty(); }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Entry<IRI, Supplier<Shape>>> labels() {
        return Map.of();
    }

    public Map<IRI, Entry<String, Supplier<Shape>>> predicates() {
        return Map.of();
    }


    public Optional<Entry<IRI, Shape>> entry(final String label) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        return Optional.ofNullable(labels().get(label)).map(e -> Map.entry(e.getKey(), e.getValue().get()));
    }

    public Optional<Entry<String, Shape>> entry(final IRI property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return Optional.ofNullable(predicates().get(property)).map(e -> Map.entry(e.getKey(), e.getValue().get()));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Trace> validate(final Frame frame) {

        if ( frame == null ) {
            throw new NullPointerException("null frame");
        }

        return Optional.empty();

        // return Optional.of(validate(Set.of(frame))).filter(not(Trace::empty));
    }


    // private Trace validate(final Collection<Value> values) {
    //     return trace(Stream
    //
    //             .of(
    //
    //                     clazz(values),
    //
    //                     predicates(values)
    //
    //             )
    //
    //             .collect(toList())
    //     );
    // }
    //
    //
    // private Trace clazz(final Collection<Value> values) {
    //     return trace(clazz().stream()
    //
    //             .flatMap(clazz -> values.stream().map(value ->
    //                     value.isResource() ? trace()
    //                             : trace(format("class(<%s>) / <%s> is not a resource", clazz, value))
    //             ))
    //
    //             .collect(toList()));
    // }
    //
    // private Trace predicates(final Collection<Value> values) {
    //     return predicates().entrySet().stream()
    //
    //             .map(e -> {
    //
    //                 final IRI predicate=e.getKey();
    //                 final Shape shape=e.getValue().getValue().get();
    //
    //                 values.stream()
    //
    //                         .map(v -> {
    //
    //                             if ( v instanceof Frame ) {
    //
    //                                 return trace();
    //
    //                             } else {
    //
    //                                 return trace();
    //
    //                             }
    //
    //
    //                         });
    //
    //
    //             });
    // }

}
