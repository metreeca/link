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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.metreeca.link.rdf4j.Coder.*;

import static java.util.stream.Collectors.toList;

final class SPARQLUpdater extends SPARQL {

    private final Collection<Task> inserts=new HashSet<>();
    private final Collection<Task> deletes=new HashSet<>();


    CompletableFuture<Void> insert(final Resource resource, final IRI predicate, final Value value) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Task(resource, predicate, value).schedule(inserts::add);

    }

    CompletableFuture<Void> delete(final Resource resource, final IRI predicate, final Value value) {

        return new Task(resource, predicate, value).schedule(deletes::add);

    }


    @Override Optional<CompletableFuture<Void>> run(final RepositoryConnection connection) {

        if ( inserts.isEmpty() && deletes.isEmpty() ) { return Optional.empty(); } else {

            return Optional.of(CompletableFuture.runAsync(() -> {

                final Collection<Task> ds=snapshot(deletes);
                final Collection<Task> is=snapshot(inserts);

                final String update=sparql(update(ds, is));

                connection.prepareUpdate(update).execute();

                ds.forEach(Task::complete);
                is.forEach(Task::complete);

            }));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder update(final Collection<Task> deletes, final Collection<Task> inserts) {
        return items(
                comment("update"),
                delete(deletes),
                insert(inserts)
        );
    }

    private Coder delete(final Collection<Task> deletes) {
        return deletes.isEmpty() ? nothing() : space(items(text("\rdelete where "), block(items(

                deletes.stream()
                        .map(delete -> {

                            final Resource resource=delete.resource;
                            final IRI predicate=delete.predicate;
                            final Value value=delete.value;

                            return line(edge(
                                    resource == null ? var(id(new Object())) : value(resource),
                                    predicate == null ? var(id(new Object())) : value(predicate),
                                    value == null ? var(id(new Object())) : value(value)
                            ));

                        })
                        .collect(toList())

        )), text(";")));
    }

    private Coder insert(final Collection<Task> inserts) {
        return inserts.isEmpty() ? nothing() : space(items(text("\rinsert data "), block(items(

                inserts.stream()
                        .map(delete -> {

                            final Resource resource=delete.resource;
                            final IRI predicate=delete.predicate;
                            final Value value=delete.value;

                            return line(edge(value(resource), iri(predicate), value(value)));

                        })
                        .collect(toList())

        )), text(";")));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Task {

        final Resource resource;
        final IRI predicate;
        final Value value;

        private final CompletableFuture<Void> future=new CompletableFuture<>();


        private Task(final Resource resource, final IRI predicate, final Value value) {
            this.resource=resource;
            this.predicate=predicate;
            this.value=value;
        }


        private CompletableFuture<Void> schedule(final Consumer<Task> queue) {

            queue.accept(this);

            return future;
        }

        private void complete() {
            future.complete(null);
        }

    }

}
