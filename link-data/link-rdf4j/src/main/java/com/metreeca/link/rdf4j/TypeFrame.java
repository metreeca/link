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

import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.specs.Query;
import com.metreeca.link.specs.Specs;
import com.metreeca.link.specs.Table;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.metreeca.link.Shape.*;
import static com.metreeca.link.rdf4j.RDF4J.*;
import static com.metreeca.link.specs.Report.report;
import static com.metreeca.link.specs.Specs.specs;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.model.util.Values.iri;

final class TypeFrame implements Type<Frame<?>> {

    @Override public CompletableFuture<Optional<Frame<?>>> lookup(
            final Reader reader, final Set<Value> values, final Frame<?> model
    ) {

        final Shape shape=model.shape();

        return values.stream()

                .filter(Value::isResource)
                .map(Resource.class::cast)

                .findFirst()

                .map(resource -> {

                    final Frame<?> frame=model.copy().id(resource.isIRI() && model.id() != null
                            ? reader.relativize(resource.stringValue())
                            : null
                    );

                    final CompletableFuture<?>[] entries=frame.entries(false)

                            .filter(e -> e.getValue() != null)

                            .map(e -> {

                                final String field=e.getKey();
                                final Object object=e.getValue();

                                final String property=shape.property(field).orElseThrow(() ->
                                        new NoSuchElementException(format("unknown field <%s>", field))
                                );

                                final Shape subshape=shape.shape(field).orElseThrow(() ->
                                        new NoSuchElementException(format("unknown field <%s>", field))
                                );

                                final Optional<String> predicate=shape.virtual()
                                        ? Optional.empty()
                                        : Optional.of(property);


                                if ( object instanceof Query ) {

                                    final Query<?> query=(Query<?>)object;

                                    final Specs _specs=query.specs();
                                    final Object _model=query.model();

                                    if ( _model instanceof Table ) {

                                        return lookup(reader, resource, predicate, subshape, _specs, ((Table)_model))
                                                .thenAccept(v -> frame.set(field, report(v)));

                                    } else {

                                        return lookup(reader, resource, predicate, subshape, _specs, _model)
                                                .thenAccept(v -> frame.set(field, v));

                                    }

                                } else if ( object instanceof Collection ) { // !!! Set/List/…?

                                    final Collection<?> collection=(Collection<?>)object;

                                    if ( collection.size() == 1 ) { // !!! review / handle missing/multiple models

                                        final Specs _specs=specs();
                                        final Object _model=collection.iterator().next();

                                        if ( _model instanceof Table ) { // !!! factor

                                            return lookup(reader, resource, predicate, subshape, _specs, ((Table)_model))
                                                    .thenAccept(v -> frame.set(field, report(v)));

                                        } else {

                                            return lookup(reader, resource, predicate, subshape, _specs, _model)
                                                    .thenAccept(v -> frame.set(field, v));

                                        }

                                    } else {

                                        return completedFuture(List.of()); // !!! report? see JSONLD.TypeObject

                                    }

                                } else {

                                    return reader.fetcher().fetch(resource, iri(forward(property)), direct(property))
                                            .thenCompose(vs -> reader.lookup(vs, object))
                                            .thenAccept(o -> frame.set(field, o.orElse(null)));

                                }

                            })

                            .toArray(CompletableFuture[]::new);

                    return allOf(entries).<Optional<Frame<?>>>thenApply(v ->
                            Optional.of(frame)
                    );

                })

                .orElse(completedFuture(Optional.empty()));
    }

    @Override public Entry<Stream<Value>, Stream<Statement>> _encode(final Writer writer, final Frame<?> value) {

        final Shape shape=value.shape();
        final ValueFactory factory=writer.factory();

        final Resource focus=Optional.ofNullable(value.id())

                .map(id -> absolute(writer.resolve(id))

                        .map(factory::createIRI)
                        .map(Resource.class::cast)

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "frame id <%s> is not an absolute IRI", id
                        )))

                )

                .orElseGet(factory::createBNode);

        final Stream<Statement> types=shape.types()

                .map(id -> absolute(id)

                        .map(factory::createIRI)

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "frame id <%s> is not an absolute IRI", id
                        )))

                )

                .map(type -> factory.createStatement(focus, RDF.TYPE, type));

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

                final Entry<Stream<Value>, Stream<Statement>> encoded=writer.encode(object);

                final Stream<Value> values=encoded.getKey();
                final Stream<Statement> statements=encoded.getValue();

                if ( direct(property) ) {

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

                                    .map(v -> factory.createStatement((Resource)v, iri(forward(property)), focus)),

                            statements
                    );

                }

            }

        });

        return entry(Stream.of(focus), Stream.concat(types, model));

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<List<Map<String, Object>>> lookup(
            final Reader reader,
            final Resource resource,
            final Optional<String> predicate,
            final Shape shape,
            final Specs specs,
            final Table model
    ) {

        return reader.selector().select(resource, predicate, shape, specs, model).thenCompose(solutions -> {

            final List<Map<String, CompletableFuture<Optional<Object>>>> records=solutions.stream()
                    .map(solution -> solution.entrySet().stream().collect(
                            () -> new LinkedHashMap<String, CompletableFuture<Optional<Object>>>(),
                            (map, entry) -> {

                                final String alias=entry.getKey();
                                final Value _value=entry.getValue();
                                final Object _model=model.get(alias).model();

                                map.put(alias, _value == null
                                        ? completedFuture(Optional.empty())
                                        : reader.lookup(Set.of(_value), _model)
                                );

                            },
                            Map::putAll
                    ))
                    .collect(toList());

            return allOf(records.stream()
                    .flatMap(record -> record.values().stream())
                    .toArray(s -> new CompletableFuture<?>[s])
            )

                    .thenApply(v -> records.stream()
                            .map(record -> record.entrySet().stream().collect(
                                    () -> new LinkedHashMap<String, Object>(),
                                    (map, entry) -> {

                                        final String alias=entry.getKey();
                                        final Object _value=entry.getValue().join().orElse(null);

                                        map.put(alias, _value);

                                    },
                                    Map::putAll
                            ))
                            .collect(toList())
                    );
        });
    }

    private CompletableFuture<List<Object>> lookup(
            final Reader reader,
            final Resource resource,
            final Optional<String> predicate,
            final Shape shape,
            final Specs specs,
            final Object model
    ) {

        return reader.selector().select(resource, predicate, shape, specs, model).thenCompose(values -> {

            final List<CompletableFuture<Optional<Object>>> objects=values.stream()
                    .map(v -> reader.lookup(Set.of(v), model))
                    .collect(toList());

            return allOf(objects.toArray(s -> new CompletableFuture<?>[s]))

                    .thenApply(v -> objects.stream()
                            .map(CompletableFuture::join)
                            .map(o -> o.orElseThrow(() ->
                                    new AssertionError(format("unable to look up item <%s>", v))
                            ))
                            .collect(toList())
                    );
        });

    }

}
