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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.link.Query.query;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Predicate.not;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.*;

/**
 * Resource description.
 */
public final class Frame implements Resource {

    private static final long serialVersionUID=-7503619785173166965L;


    private static final ValueFactory FACTORY=new AbstractValueFactory() { };
    private static final Comparator<Value> COMPARATOR=new ValueComparator();

    private static final Pattern LOCALE_PATTERN=compile("(?:x|[a-zA-Z]\\w+)(-\\w+)*");


    static int compare(final Value x, final Value y) {
        return COMPARATOR.compare(x, y);
    }


    //// JSON-LD ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final IRI ID=RDF.NIL;
    public static final IRI TYPE=RDF.TYPE;

    public static final String _ID="@id";
    public static final String _TYPE="@type";
    public static final String _VALUE="@value";
    public static final String _LANGUAGE="@language";
    public static final String _ERRORS="@errors";

    public static final Value NIL=RDF.NIL;

    public static final String WILDCARD="*";


    //// Generalized RDF Datatypes /////////////////////////////////////////////////////////////////////////////////////

    public static final IRI VALUE=iri(RDF.NAMESPACE, "x-value"); // abstract datatype IRI for values
    public static final IRI RESOURCE=iri(RDF.NAMESPACE, "x-resource"); // abstract datatype IRI for resources
    public static final IRI BNODE=iri(RDF.NAMESPACE, "x-bnode"); // datatype IRI for blank nodes
    public static final IRI IRI=iri(RDF.NAMESPACE, "x-iri"); // datatype IRI for IRI references
    public static final IRI LITERAL=iri(RDF.NAMESPACE, "x-literal"); // abstract datatype IRI for literals


    //// Reverse properties ////////////////////////////////////////////////////////////////////////////////////////////

    private static final String REVERSE_SCHEME="reverse:";


    /**
     * Checks property direction.
     *
     * @param property the IRI identifying the property
     *
     * @return {@code true} if {@code property} identifies a forward property; {@code false} if {@code property}
     * identifies an {@link #reverse(IRI) reverse} property
     *
     * @throws NullPointerException if {@code property} is null
     */
    public static boolean forward(final IRI property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return !property.stringValue().startsWith(REVERSE_SCHEME);
    }

    /**
     * Reverses a property.
     *
     * @param property the IRI identifying the property to be reversed
     *
     * @return the reversed version of {@code property}
     *
     * @throws NullPointerException if {@code property} is null
     */
    public static IRI reverse(final IRI property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        final String iri=property.stringValue();

        return iri.startsWith(REVERSE_SCHEME)
                ? iri(iri.substring(REVERSE_SCHEME.length()))
                : iri(REVERSE_SCHEME+iri);
    }


    //// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Frame frame(final Field... fields) {

        if ( fields == null || Arrays.stream(fields).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null fields");
        }

        return frame(asList(fields));
    }

    public static Frame frame(final Collection<Field> fields) {

        if ( fields == null || fields.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null fields");
        }

        return new Frame(fields.stream()
                .filter(not(field -> field.values().isEmpty()))
                .collect(groupingBy(
                        Field::property,
                        LinkedHashMap::new,
                        mapping(Field::values, flatMapping(Collection::stream, toCollection(LinkedHashSet::new)))
                ))
        );
    }


    public static Field field(final IRI predicate, final Value value) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Field(predicate, Set.of(value));
    }

    public static Field field(final IRI predicate, final Optional<? extends Value> value) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Field(predicate, value.stream().collect(toUnmodifiableSet()));
    }


    public static Field field(final IRI predicate, final Value... values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return new Field(predicate, asList(values));
    }

    public static Field field(final IRI predicate, final Stream<? extends Value> values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return new Field(predicate, values.peek(value -> {

            if ( value == null ) {
                throw new NullPointerException("null values");
            }

        }).collect(toList()));
    }

    public static Field field(final IRI predicate, final Collection<? extends Value> values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        if ( !forward(predicate) && values.stream().anyMatch(not(Value::isResource)) ) {
            throw new IllegalArgumentException(format("literal values for reverse predicate <%s>", predicate));
        }

        return new Field(predicate, values);
    }


    public static BNode bnode() {
        return FACTORY.createBNode();
    }

    public static BNode bnode(final String id) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        return FACTORY.createBNode(id.startsWith("_:") ? id.substring(2) : id);
    }


    public static IRI iri() {
        return FACTORY.createIRI("urn:uuid:", UUID.randomUUID().toString());
    }

    public static IRI iri(final URI uri) {

        if ( uri == null ) {
            throw new NullPointerException("null uri");
        }

        return FACTORY.createIRI(uri.toString());
    }

    public static IRI iri(final URL url) {

        if ( url == null ) {
            throw new NullPointerException("null url");
        }

        return FACTORY.createIRI(url.toString());
    }

    public static IRI iri(final String iri) {

        if ( iri == null ) {
            throw new NullPointerException("null iri");
        }

        return FACTORY.createIRI(iri);
    }

    public static IRI iri(final IRI space, final String name) {

        if ( space == null ) {
            throw new NullPointerException("null space");
        }

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return iri(space.stringValue(), name);
    }

    public static IRI iri(final String space, final String name) {

        if ( space == null ) {
            throw new NullPointerException("null space");
        }

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return FACTORY.createIRI(space,
                space.endsWith("/") && name.startsWith("/") ? name.substring(1) : name
        );
    }


    public static Literal literal(final boolean value) {
        return FACTORY.createLiteral(value);
    }


    public static Literal literal(final BigDecimal value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final BigInteger value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final double value) {
        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final float value) {
        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final long value) {
        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final int value) {
        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final short value) {
        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final byte value) {
        return FACTORY.createLiteral(value);
    }


    public static Literal literal(final String value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return FACTORY.createLiteral(value);
    }


    public static Literal literal(final TemporalAccessor value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return FACTORY.createLiteral(value);
    }

    public static Literal literal(final TemporalAmount value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return FACTORY.createLiteral(value);
    }


    public static Literal literal(final byte[] value) { return literal(value, "application/octet-stream"); }

    public static Literal literal(final byte[] value, final String mime) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( mime == null ) {
            throw new NullPointerException("null mime");
        }

        return FACTORY.createLiteral(
                format("data:%s;base64,%s", mime, Base64.getEncoder().encodeToString(value)),
                XSD.ANYURI
        );
    }


    public static Literal literal(final String value, final IRI datatype) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( datatype == null ) {
            throw new NullPointerException("null datatype");
        }

        return FACTORY.createLiteral(value, datatype);
    }

    public static Literal literal(final String value, final Locale locale) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        return literal(value, locale.getLanguage());
    }

    public static Literal literal(final String value, final String locale) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( locale.isEmpty() ) {

            return FACTORY.createLiteral(value);

        } else if ( locale.equals(WILDCARD) ) {

            return FACTORY.createLiteral(value, WILDCARD);

        } else {

            if ( !LOCALE_PATTERN.matcher(locale).matches() ) {
                throw new IllegalArgumentException(format("malformed language tag <%s>", locale));
            }

            return FACTORY.createLiteral(value, locale);

        }
    }


    public static BigInteger integer(final long value) {
        return BigInteger.valueOf(value);
    }

    public static BigDecimal decimal(final double value) {
        return BigDecimal.valueOf(value);
    }


    public static <T> T error(final String format, final Object... args) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        if ( args == null ) {
            throw new NullPointerException("null args");
        }

        throw new IllegalArgumentException(format(format, args));
    }


    //// Converters ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Function<Value, Frame> asFrame() {
        return value -> value instanceof Frame ? (Frame)value : null;
    }

    public static Function<Value, IRI> asIRI() {
        return value -> value instanceof IRI ? (IRI)value : null;
    }


    public static Function<Value, BigDecimal> asDecimal() {
        return value -> value instanceof Literal ? ((Literal)value).decimalValue() : null;
    }

    public static Function<Value, BigInteger> asInteger() {
        return value -> value instanceof Literal ? ((Literal)value).integerValue() : null;
    }

    public static Function<Value, Double> asDouble() {
        return value -> value instanceof Literal ? ((Literal)value).doubleValue() : null;
    }

    public static Function<Value, Float> asFloat() {
        return value -> value instanceof Literal ? ((Literal)value).floatValue() : null;
    }

    public static Function<Value, Long> asLong() {
        return value -> value instanceof Literal ? ((Literal)value).longValue() : null;
    }

    public static Function<Value, Integer> asInt() {
        return value -> value instanceof Literal ? ((Literal)value).intValue() : null;
    }

    public static Function<Value, Short> asShort() {
        return value -> value instanceof Literal ? ((Literal)value).shortValue() : null;
    }

    public static Function<Value, Byte> asByte() {
        return value -> value instanceof Literal ? ((Literal)value).byteValue() : null;
    }


    public static Function<Value, LocalDate> asLocalDate() {
        return value -> value instanceof Literal ? LocalDate.from(((Literal)value).temporalAccessorValue()) : null;
    }

    public static Function<Value, TemporalAccessor> asTemporalAccessor() {
        return value -> value instanceof Literal ? ((Literal)value).temporalAccessorValue() : null;
    }

    public static Function<Value, TemporalAmount> asTemporalAmount() {
        return value -> value instanceof Literal ? ((Literal)value).temporalAmountValue() : null;
    }


    public static Function<Value, String> asString() {
        return value -> value instanceof Literal ? value.stringValue() : null;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<IRI, Set<Value>> fields;


    private Frame(final Map<IRI, Set<Value>> fields) {
        this.fields=unmodifiableMap(fields);
    }


    public boolean tabular() {
        return fields.keySet().stream().anyMatch(Probe.class::isInstance);
    }


    public Optional<IRI> id() {
        return value(ID).filter(Value::isIRI).map(IRI.class::cast);
    }

    public Map<IRI, Set<Value>> fields() {
        return fields;
    }


    public Optional<Value> value(final IRI property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return values(property).findFirst();
    }

    public Stream<Value> values(final IRI property) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return Optional.ofNullable(fields.get(property)).stream().flatMap(Collection::stream);
    }


    public <T> Optional<T> value(final IRI property, final Function<Value, T> converter) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return value(property).map(guard(converter));
    }

    public <T> Stream<T> values(final IRI property, final Function<Value, T> converter) {

        if ( property == null ) {
            throw new NullPointerException("null property");
        }

        return values(property).map(guard(converter)).filter(Objects::nonNull);
    }


    private <T> Function<Value, T> guard(final Function<Value, T> converter) {
        return value -> {

            try {

                return converter.apply(value);

            } catch ( final RuntimeException ignored ) {

                return null;

            }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public Stream<Statement> stream() {
        return stream(id()
                .map(Resource.class::cast)
                .orElseGet(FACTORY::createBNode)
        );
    }


    private Stream<Statement> stream(final Resource subject) {
        return fields.entrySet().stream().flatMap(e -> {

            final IRI iri=e.getKey();
            final Set<Value> values=e.getValue();

            final boolean forward=forward(iri);

            final IRI predicate=forward ? iri : reverse(iri);

            return values.stream().flatMap(value -> {

                if ( value instanceof Frame ) {

                    final Frame frame=(Frame)value;

                    final Resource object=frame.id()
                            .map(Resource.class::cast)
                            .orElseGet(FACTORY::createBNode);

                    return Stream.concat(

                            Stream.of(forward
                                    ? FACTORY.createStatement(subject, predicate, object)
                                    : FACTORY.createStatement(object, predicate, subject)
                            ),

                            frame.stream(object)

                    );

                } else {

                    return Stream.of(forward
                            ? FACTORY.createStatement(subject, predicate, value)
                            : FACTORY.createStatement((Resource)value, predicate, subject)
                    );

                }

            });

        });
    }


    public Frame merge(final Frame frame) {

        if ( frame == null ) {
            throw new NullPointerException("null frame");
        }

        return merge(this, frame);
    }


    private Query merge(final Query query, final Query _query) {
        return query(merge(query.model(), _query.model()), query, _query);
    }

    private Query merge(final Query query, final Frame _frame) {
        return query(merge(query.model(), _frame), query);
    }

    private Query merge(final Frame frame, final Query _query) {
        return query(merge(frame, _query.model()), _query);
    }

    private Frame merge(final Frame frame, final Frame _frame) {
        if ( frame.fields.isEmpty() ) { return _frame; } else {

            final Map<IRI, Set<Value>> fields=new LinkedHashMap<>();

            frame.fields.forEach((predicate, values) -> fields.put(predicate, values.stream()

                    .flatMap(value -> Optional.ofNullable(_frame.fields.get(predicate))

                            .map(_values -> _values.stream().map(_value -> {

                                if ( value instanceof Query ) {

                                    return _value instanceof Query ? merge((Query)value, (Query)_value)
                                            : _value instanceof Frame ? merge((Query)value, (Frame)_value)
                                            : value;

                                } else if ( value instanceof Frame ) {

                                    return _value instanceof Query ? merge((Frame)value, (Query)_value)
                                            : _value instanceof Frame ? merge((Frame)value, (Frame)_value)
                                            : value;

                                } else {

                                    return _value; // replace plain model value to support virtual resources

                                }

                            }))

                            .orElseGet(() -> Stream.of(value))
                    )

                    .collect(toUnmodifiableSet())

            ));

            return new Frame(fields);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public String stringValue() {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        return fields.isEmpty() ? "{}" : fields.entrySet().stream()

                .map(field -> field.getValue().stream()
                        .map(Object::toString) // !!! format
                        .collect(joining(",\n\t", format("<%s> : ", field.getKey()), ""))
                )

                .collect(joining(",\n\t", "{\n\t", "\n}"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Frame field.
     */
    public static final class Field {

        private final IRI property;
        private final Collection<? extends Value> values;


        Field(final IRI property, final Collection<? extends Value> values) {
            this.property=property;
            this.values=values;
        }


        private IRI property() {
            return property;
        }

        private Collection<? extends Value> values() {
            return values;
        }

    }

}
