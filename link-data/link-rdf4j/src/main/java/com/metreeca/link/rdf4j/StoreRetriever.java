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

package com.metreeca.link.rdf4j;

import com.metreeca.link.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.rdf4j.SPARQL.join;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

final class StoreRetriever {

    private final RDF4J.Context context;

    private final SPARQLFetcher fetcher;
    private final SPARQLSelector selector;


    StoreRetriever(final RDF4J.Context context) {

        this.context=context;

        this.fetcher=context.worker(SPARQLFetcher::new);
        this.selector=context.worker(SPARQLSelector::new);
    }


    Optional<Frame> retrieve(final Resource id, final Shape shape, final Frame model) {
        return context.execute(() -> process(id, shape, model)).join();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Optional<Frame>> process(final Resource id, final Shape shape, final Frame model) {

        final boolean virtual=shape.virtual();

        // generate field futures before checking for resource existence, so that a single query is generated

        final Collection<CompletableFuture<Field>> futures=model.fields().entrySet().stream().flatMap(entry -> {

            final IRI property=entry.getKey();
            final Set<Value> values=entry.getValue();

            if ( property.equals(ID) ) {

                return Stream.of(completedFuture(field(property, id)));

            } else {

                return shape.entry(property)
                        .map(Map.Entry::getValue)
                        .stream()
                        .flatMap(nested -> values.stream().map(value ->
                                value instanceof Query ? process(id, property, virtual, nested, (Query)value)
                                        : value instanceof Probe ? process(id, property, virtual, nested, (Probe)value)
                                        : process(id, property, virtual, nested, value)
                        ));

            }

        }).collect(toList());

        return (virtual

                ? completedFuture(true)
                : fetcher.fetch(id)

        ).thenCompose(exists -> exists

                ? join(futures).thenApply(fields -> Optional.of(frame(fields)))
                : completedFuture(Optional.empty())

        );

    }


    private CompletableFuture<Field> process(
            final Resource id, final IRI property, final boolean virtual, final Shape shape, final Value model
    ) {
        if ( virtual ) {

            return completedFuture(field(property, model));

        } else if ( model instanceof Frame ) {

            return fetcher.fetch(id, property)

                    .thenCompose(values -> join(values.stream()
                            .filter(Resource.class::isInstance)
                            .map(value -> process((Resource)value, shape, (Frame)model)
                                    .thenApply(o -> o.orElseGet(() -> frame(field(ID, value))))
                            )
                    ))

                    .thenApply(frames -> field(property, frames));

        } else {

            return fetcher.fetch(id, property)

                    .thenApply(values -> field(property, values));

        }
    }

    private CompletableFuture<Field> process(
            final Resource id, final IRI property, final boolean virtual, final Shape shape, final Probe probe
    ) {
        throw new UnsupportedOperationException("probe value outside query model"); // !!!
    }

    private CompletableFuture<Field> process(
            final Resource id, final IRI property, final boolean virtual, final Shape shape, final Query query
    ) {

        final Frame model=query.model();

        final boolean probing=model.fields().keySet().stream().anyMatch(Probe.class::isInstance);

        if ( probing ) {

            final Map<IRI, Expression> fields=new LinkedHashMap<>();

            model.fields().keySet().forEach(p -> {

                if ( p instanceof Probe ) {
                    fields.put(p, ((Probe)p).expression());
                } else {
                    fields.put(p, expression(p));
                }

            });

            return selector.select(id, property, virtual, query, fields)

                    .thenCompose(tuples -> join(tuples.stream()

                            .map(tuple -> join(tuple.entrySet().stream()

                                            .map(field -> {

                                                final IRI p=field.getKey();
                                                final Value v=field.getValue();

                                                return join(model.values(p).map(m -> { // !!! refactor

                                                    if ( m instanceof Frame ) {

                                                        if ( v.isResource() ) {

                                                            return process((Resource)v, shape, (Frame)m)
                                                                    .thenApply(o -> o.orElseGet(() -> frame(field(ID, v))));

                                                        } else {

                                                            return completedFuture(v);

                                                        }


                                                    } else {

                                                        return completedFuture(v);

                                                    }

                                                }))

                                                        .thenApply(vs -> field(p, vs));

                                            })

                                    )
                                            .thenApply(fs -> frame(fs.collect(toList())))

                            )))


                    .thenApply(frames -> field(property, frames));

        } else {

            return selector.select(id, property, virtual, query)

                    .thenCompose(values -> join(values.stream()
                            .filter(Resource.class::isInstance)
                            .map(value -> process((Resource)value, shape, model)
                                    .thenApply(o -> o.orElseGet(() -> frame(field(ID, value))))
                            )
                    ))

                    .thenApply(frames -> field(property, frames));

        }

    }

}
