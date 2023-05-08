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

package com.metreeca.link.rdf4j;

import com.metreeca.link.*;
import com.metreeca.link.Table.Column;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.absolute;
import static com.metreeca.link.Local.local;
import static com.metreeca.link.Query.query;
import static com.metreeca.link.Shape.forward;
import static com.metreeca.link.Shape.reverse;
import static com.metreeca.link.rdf4j.Coder.*;
import static com.metreeca.link.rdf4j.RDF4J.*;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;
import static org.eclipse.rdf4j.model.util.Values.iri;

final class TypeFrame implements Type<Frame<?>> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Frame<?> value) {

        final Shape shape=value.shape();
        final ValueFactory factory=encoder.factory();

        final Resource focus=Optional.ofNullable(value.id())

                .map(id -> absolute(encoder.resolve(id))

                        .map(factory::createIRI)
                        .map(Resource.class::cast)

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "frame id <%s> is not an absolute IRI", id
                        )))

                )

                .orElseGet(factory::createBNode);

        final Stream<IRI> types=shape.types()

                .map(id -> absolute(id)

                        .map(factory::createIRI)

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "frame id <%s> is not an absolute IRI", id
                        )))

                );


        final Stream<Statement> model=value.entries(false).flatMap(entry -> {

            final String field=entry.getKey();
            final Object object=entry.getValue();

            if ( object == null ) {

                return Stream.empty();

            } else {

                final String property=shape.property(field)

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "unknown frame field <%s>", field
                        )));

                final Entry<Stream<Value>, Stream<Statement>> encoded=encoder.encode(object);

                final Stream<Value> values=encoded.getKey();
                final Stream<Statement> statements=encoded.getValue();

                if ( forward(property) ) {

                    return Stream.concat(
                            values.map(v -> factory.createStatement(focus, iri(property), v)),
                            statements
                    );

                } else {

                    return Stream.concat(

                            values

                                    .peek(v -> {

                                        if ( !v.isResource() ) {

                                            throw new IllegalArgumentException(format(
                                                    "value <%s> for reverse property <%s> is not a resource", v, property
                                            ));

                                        }

                                    })

                                    .map(v -> factory.createStatement((Resource)v, iri(reverse(property)), focus)),

                            statements
                    );

                }

            }

        });

        return entry(Stream.of(focus), Stream.concat(
                types.map(type -> factory.createStatement(focus, RDF.TYPE, type)),
                model
        ));

    }

    @Override public Optional<Frame<?>> decode(final Decoder decoder, final Value value, final Frame<?> model) {

        final Shape shape=model.shape();
        final boolean virtual=shape.virtual();

        final RepositoryConnection connection=decoder.connection();

        return Optional.of(value)

                .filter(Value::isResource)
                .map(Resource.class::cast)

                .filter(resource -> virtual
                        || connection.hasStatement(resource, null, null, true)
                        || connection.hasStatement(null, null, resource, true)
                )

                .map(resource -> {

                    final Frame<?> frame=model.copy();

                    frame.entries(false).forEach(e -> {

                        final String field=e.getKey();
                        final Object object=e.getValue();

                        if ( object != null ) {

                            final String property=shape.property(field).orElseThrow(() ->
                                    new NoSuchElementException(format("unknown field <%s>", field))
                            );

                            final Shape subshape=shape.shape(field).orElseThrow(() ->
                                    new NoSuchElementException(format("unknown field <%s>", field))
                            );

                            frame.set(field, decode(decoder, shape, resource, property, object, subshape));

                        }

                    });

                    return frame.id(decoder.relativize(frame.id()));

                });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Collection<Frame<?>> decode(final Decoder decoder, final Frame<?> model, final Collection<Resource> resources) {

        final List<Entry<String, Object>> scalars=model.entries(false)

                .filter(not(e -> e.getValue() == null))
                .filter(not(e -> e.getValue() instanceof Collection))

                .collect(toList());

        final List<Entry<String, Object>> collections=model.entries(false)

                .filter(not(e -> e.getValue() == null))
                .filter(e -> e.getValue() instanceof Collection)

                .collect(toList());

        // frame.id(decoder.relativize(frame.id()));

        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Object decode(
            final Decoder decoder, final Shape shape,
            final Resource resource, final String property, final Object object, final Shape subshape
    ) {

        return object instanceof Query ? decode(decoder, shape, resource, property, (Query<?>)object, subshape)
                : object instanceof List ? decode(decoder, resource, property, (List<?>)object)
                : object instanceof Collection ? decode(decoder, shape, resource, property, (Collection<?>)object, subshape)
                : object instanceof Local ? decode(decoder, shape, resource, property, (Local<?>)object)
                : decode(decoder, shape, resource, property, object);
    }

    private static Object decode(
            final Decoder decoder, final Shape shape,
            final Resource resource, final String property, final Query<?> query, final Shape subshape
    ) {

        final RepositoryConnection connection=decoder.connection();

        final Object model=query.model();

        final SPARQLMembers sparql=new SPARQLMembers();

        final String members=sparql.members(
                resource,
                shape.virtual() ? Optional.empty() : Optional.of(property),
                subshape,
                query
        );


        if ( model instanceof Table ) {

            final Table<?> table=(Table<?>)model;
            final Table<?> copy=table.copy();
            final Map<String, Column> columns=copy.columns();

            final List<Map<String, Value>> solutions=select(connection, members, bindings ->
                    columns.keySet().stream().collect(
                            HashMap::new, // ;( handle null values
                            (map, alias) -> map.put(alias, bindings.getValue(alias)),
                            Map::putAll

                    )
            );

            solutions.forEach(solution -> copy.append(solution.entrySet().stream().collect(
                    LinkedHashMap::new,
                    (map, entry) -> {

                        final String alias=entry.getKey();
                        final Value _value=entry.getValue();
                        final Object _template=columns.get(alias).template();

                        map.put(alias, decoder
                                .decode(_value, _template) // !!! batch retrieval
                                .orElse(null)
                        );

                    },
                    Map::putAll
            )));

            return copy;

        } else {

            final List<Value> values=select(connection, members, bindings ->
                    bindings.getValue(sparql.id())
            );

            return values.stream()
                    .flatMap(v -> decoder.decode(v, model).stream()) // !!! batch retrieval
                    .collect(toList());
        }

    }

    private static List<?> decode(
            final Decoder decoder, final Shape shape,
            final Resource resource, final String property, final Collection<?> collection, final Shape subshape
    ) {

        // !!! migrate to TypeCollection?

        final RepositoryConnection connection=decoder.connection();

        final SPARQLMembers generator=new SPARQLMembers();

        final String members=generator.members(
                resource,
                shape.virtual() ? Optional.empty() : Optional.of(property),
                subshape,
                query()
        );

        final List<Value> values=select(connection, members, bindings ->
                bindings.getValue(generator.id())
        );

        return values.stream() // !!! batch retrieval
                .flatMap(v -> collection.stream()
                        .flatMap(o -> decoder.decode(v, o).stream())
                )
                .collect(toList());
    }

    private static Object decode(
            final Decoder decoder,
            final Resource resource, final String property, final List<?> model
    ) {
        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }

    private static Object decode(
            final Decoder decoder, final Shape shape,
            final Resource resource, final String property, final Local<?> local
    ) {

        // !!! migrate to TypeLocal?

        final RepositoryConnection connection=decoder.connection();

        final boolean empty=local.values().keySet().isEmpty();
        final boolean wild=local.values().keySet().stream().anyMatch(Local.Wildcard::equals);
        final boolean unique=local.values().values().stream().anyMatch(String.class::isInstance);

        final Set<String> locales=local.values().keySet().stream()
                .map(Locale::toLanguageTag)
                .collect(toSet());

        final Stream<Literal> literals=retrieve(
                connection,
                resource,
                property,
                shape.links().collect(toList()),
                vs -> vs.collect(toList())

        ).stream()
                .filter(Value::isLiteral)
                .map(Literal.class::cast)
                .filter(v -> v.getLanguage().isPresent())
                .filter(v -> empty || wild || v.getLanguage().filter(locales::contains).isPresent());

        return unique

                ?
                local(literals
                        .map(v -> local(v.getLanguage().get(), v.stringValue()))
                        .collect(toList())
                )

                :
                local(literals
                        .collect(groupingBy(v -> v.getLanguage().get(), mapping(Value::stringValue, toSet())))
                        .entrySet()
                        .stream()
                        .map(entry -> local(entry.getKey(), entry.getValue()))
                        .collect(toList())
                );
    }

    private static Object decode(
            final Decoder decoder, final Shape shape,
            final Resource resource, final String property, final Object object
    ) {

        // !!! batch retrieval

        final RepositoryConnection connection=decoder.connection();

        return retrieve(connection, resource, property, shape.links().collect(toList()), Stream::findFirst)
                .flatMap(v -> decoder.decode(v, object))
                .orElse(shape.virtual() ? object : null);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <V> List<V> select(
            final RepositoryConnection connection,
            final String query,
            final Function<BindingSet, V> mapper
    ) {

        try ( final Stream<BindingSet> results=connection.prepareTupleQuery(query).evaluate().stream() ) {

            return results.map(mapper).collect(toList());

        }

    }


    private static <V> V retrieve(
            final RepositoryConnection connection,
            final Resource anchor,
            final String predicate,
            final Collection<String> links,
            final Function<Stream<Value>, V> mapper
    ) {

        final boolean forward=forward(predicate);

        if ( links.isEmpty() ) {

            try ( final Stream<Statement> statements=forward
                    ? connection.getStatements(anchor, iri(predicate), null).stream()
                    : connection.getStatements(null, iri(reverse(predicate)), anchor).stream()
            ) {

                return mapper.apply(forward
                        ? statements.map(Statement::getObject)
                        : statements.map(Statement::getSubject)
                );

            }

        } else {

            final class Generator extends SPARQL {

                private String generate() {
                    return query(items(

                            select(var(id())),

                            where(space(edge(

                                    value(anchor),

                                    items(
                                            text("("),
                                            list("|", links.stream().map(this::iri).collect(toList())),
                                            text(")?/"),
                                            iri(predicate)
                                    ),

                                    var(id())

                            )))

                    ));
                }

            }

            final Generator generator=new Generator();
            final String sparql=generator.generate();

            try ( final Stream<BindingSet> results=connection.prepareTupleQuery(sparql).evaluate().stream() ) {
                return mapper.apply(results.map(bindings -> bindings.getValue(generator.id())));
            }

        }

    }

}
