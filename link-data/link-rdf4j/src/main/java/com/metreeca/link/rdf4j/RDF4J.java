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

import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toList;

/**
 * RDF4J graph storage driver.
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class RDF4J implements Store {

    public static RDF4J rdf4j(final Repository repository) {

        if ( repository == null ) {
            throw new NullPointerException("null repository");
        }

        return new RDF4J(repository);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Repository repository;


    private RDF4J(final Repository repository) {
        this.repository=repository;
    }

    // !!! target contexts


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RDF4J shape(final Shape shape, final IRI context) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        final Collection<Statement> model=new SHACLCodec().encode(shape).collect(toList());

        try ( final RepositoryConnection connection=repository.getConnection() ) {

            try { // !!! review ontotext-specific assumptions

                connection.begin();

                connection.clear(context);
                connection.add(model, context);
                // !!! connection.add(this.context != null ? this.context : NIL, SHACL.SHAPES_GRAPH, context, SHACL_SHAPE_GRAPH);
                connection.commit();

            } catch ( final Throwable t ) {

                connection.rollback();

                throw t;

            }

        }

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override public Optional<Frame> retrieve(final IRI id, final Shape shape, final Frame model, final List<String> langs) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( langs == null || langs.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        try ( final RepositoryConnection connection=repository.getConnection() ) { // !!! pool

            return new StoreRetriever(new Context(connection)).retrieve(id, shape, model);

        }
    }


    @Override public boolean create(final IRI id, final Shape shape, final Frame state) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( state == null ) {
            throw new NullPointerException("null frame");
        }

        try ( final RepositoryConnection connection=repository.getConnection() ) { // !!! pool

            return new StoreCreator(new Context(connection)).create(id, shape, state);

        }
    }

    @Override public boolean update(final IRI id, final Shape shape, final Frame state) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( state == null ) {
            throw new NullPointerException("null frame");
        }

        try ( final RepositoryConnection connection=repository.getConnection() ) { // !!! pool

            return new StoreUpdater(new Context(connection)).update(id, state);

        }
    }

    @Override public boolean delete(final IRI id, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        try ( final RepositoryConnection connection=repository.getConnection() ) { // !!! pool

            return new StoreDeleter(new Context(connection)).delete(id);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class Context {

        private final RepositoryConnection connection;

        private final Map<Supplier<? extends SPARQL>, SPARQL> workers=new HashMap<>(); // async workers by factory


        private Context(final RepositoryConnection connection) {
            this.connection=connection;
        }


        RepositoryConnection connection() { // !!! access only through workers
            return connection;
        }

        @SuppressWarnings("unchecked") <T extends SPARQL> T worker(final Supplier<T> factory) {
            return (T)workers.computeIfAbsent(factory, Supplier::get);
        }


        <T> T execute(final Supplier<T> task) {

            final T value=task.get();

            CompletableFuture<?>[] batch;

            do {

                batch=workers.values().stream()
                        .flatMap(v -> v.run(connection).stream())
                        .toArray(CompletableFuture[]::new);

                allOf(batch).join();

            } while ( batch.length > 0 );

            return value;

        }

    }

}
