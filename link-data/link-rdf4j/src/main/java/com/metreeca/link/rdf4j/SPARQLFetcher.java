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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.link.Shape.*;
import static com.metreeca.link.rdf4j.Coder.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static org.eclipse.rdf4j.model.util.Values.getValueFactory;

final class SPARQLFetcher extends SPARQL {

    private static final String Self=format("java:%s#self", SPARQLFetcher.class.getName());

    private static final String Subject=Self;
    private static final String Object=reverse(Self);

    private static final Literal False=getValueFactory().createLiteral(false);
    private static final Literal True=getValueFactory().createLiteral(true);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Key, CompletableFuture<Set<Value>>> edges=new HashMap<>();


    CompletableFuture<Boolean> fetch(final Value resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        final CompletableFuture<Set<Value>> subject=fetch(resource, Values.iri(Self), direct(Subject));
        final CompletableFuture<Set<Value>> object=fetch(resource, Values.iri(forward(Object)), direct(Object));

        return subject.thenCombine(object, (s, o) -> s.contains(True) || o.contains(True));
    }

    CompletableFuture<Set<Value>> fetch(final Value resource, final IRI predicate, final boolean direct) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return edges.computeIfAbsent(new Key(resource, predicate, direct), key -> new CompletableFuture<>());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override Optional<CompletableFuture<Void>> run(final RepositoryConnection connection) {

        return Optional

                // identify pending edges

                .of(edges.entrySet().stream()
                        .filter((entry -> !entry.getValue().isDone()))
                        .map(Map.Entry::getKey)
                        .collect(toList())
                )

                .filter(Predicate.not(List::isEmpty))

                .map(pending -> CompletableFuture.runAsync(() -> {

                    // collect matching values

                    final TupleQuery query=connection.prepareTupleQuery(generate(pending));

                    try ( final Stream<BindingSet> tuples=query.evaluate().stream() ) {

                        tuples.collect(groupingBy(

                                tuple -> new Key(
                                        tuple.getValue("r"),
                                        (IRI)tuple.getValue("p"),
                                        tuple.getValue("f").equals(True)
                                ),

                                mapping(tuple -> tuple.getValue("v"), toSet())

                        )).forEach((key, values) -> Optional.ofNullable(edges.get(key))
                                .map(future -> future.complete(values))
                                .orElseThrow(() -> new AssertionError(format("missing edge <%s>", key)))
                        );

                    }

                    // provide empty value set for unmatched keys

                    pending.stream()
                            .map(edges::get)
                            .filter((values -> !values.isDone()))
                            .forEach(values -> values.complete(Set.of()));

                }));

    }


    private String generate(final Collection<Key> pending) {

        final List<List<? extends Value>> subjects=pending.stream()
                .filter(key -> key.forward)
                .filter(key -> key.predicate.stringValue().equals(Self))
                .map(key -> List.of(key.resource, key.predicate, True))
                .collect(toList());

        final List<List<? extends Value>> objects=pending.stream()
                .filter(key -> !key.forward)
                .filter(key -> key.predicate.stringValue().equals(Self))
                .map(key -> List.of(key.resource, key.predicate, False))
                .collect(toList());

        final List<List<? extends Value>> forwards=pending.stream()
                .filter(key -> key.forward)
                .filter(key -> !key.predicate.stringValue().equals(Self))
                .map(key -> List.of(key.resource, key.predicate, True))
                .collect(toList());

        final List<List<? extends Value>> reverses=pending.stream()
                .filter(key -> !key.forward)
                .filter(key -> !key.predicate.stringValue().equals(Self))
                .map(key -> List.of(key.resource, key.predicate, False))
                .collect(toList());

        final List<Coder> vars=List.of(var("r"), var("p"), var("f"));

        return sparql(items(
                select(var("r"), var("p"), var("v"), var("f")),
                where(

                        space(union(

                                subjects.isEmpty() ? nothing() : items(
                                        space(values(vars, subjects)),
                                        space(bind(exists(items(var("r"), var("x"), var("y"))), "v"))
                                ),

                                objects.isEmpty() ? nothing() : items(
                                        space(values(vars, objects)),
                                        space(bind(exists(items(var("x"), var("y"), var("r"))), "v"))
                                ),

                                forwards.isEmpty() ? nothing() : items(
                                        space(values(vars, forwards)),
                                        space(edge(var("r"), var("p"), var("v")))
                                ),

                                reverses.isEmpty() ? nothing() : items(
                                        space(values(vars, reverses)),
                                        space(edge(var("v"), var("p"), var("r")))
                                )

                        ))

                )
        ));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Key {

        final boolean forward;

        final Value resource;
        final IRI predicate;


        private Key(final Value resource, final IRI predicate, final boolean forward) {
            this.forward=forward;
            this.resource=resource;
            this.predicate=predicate;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Key
                    && forward == ((Key)object).forward
                    && resource.equals(((Key)object).resource)
                    && predicate.equals(((Key)object).predicate);
        }

        @Override public int hashCode() {
            return Boolean.hashCode(forward)
                    ^resource.hashCode()
                    ^predicate.hashCode();
        }

        @Override public String toString() {
            return forward
                    ? format("<%s> <%s> <?>", resource.stringValue(), predicate.stringValue())
                    : format("<?> <%s> <%s>", predicate.stringValue(), resource.stringValue());
        }

    }

}
