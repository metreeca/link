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

import com.metreeca.link.Engine;
import com.metreeca.link.Frame;
import com.metreeca.link.Local;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.link.Shape.root;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.vocabulary.RDF4J.NIL;
import static org.eclipse.rdf4j.model.vocabulary.RDF4J.SHACL_SHAPE_GRAPH;

/**
 * RDF4J graph storage driver.
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class RDF4J implements Engine {

    public static RDF4J rdf4j(final Repository repository) {

        if ( repository == null ) {
            throw new NullPointerException("null repository");
        }

        return new RDF4J(repository);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Repository repository;

    private IRI context;

    private List<Entry<Class<?>, Type<?>>> types=List.of(

            entry(Boolean.TYPE, new TypeBoolean()),
            entry(Boolean.class, new TypeBoolean()),

            entry(BigDecimal.class, new TypeBigDecimal()),
            entry(BigInteger.class, new TypeBigInteger()),

            entry(Integer.TYPE, new TypeInteger()),
            entry(Integer.class, new TypeInteger()),

            entry(Year.class, new TypeYear()),
            entry(LocalDate.class, new TypeLocalDate()),
            entry(LocalTime.class, new TypeLocalTime()),
            entry(OffsetTime.class, new TypeOffsetTime()),
            entry(LocalDateTime.class, new TypeLocalDateTime()),
            entry(OffsetDateTime.class, new TypeOffsetDateTime()),
            entry(Instant.class, new TypeInstant()),
            entry(Period.class, new TypePeriod()),
            entry(Duration.class, new TypeDuration()),

            entry(URI.class, new TypeURI()),

            entry(String.class, new TypeString()),
            entry(Local.class, new TypeLocal()),

            entry(Frame.class, new TypeFrame()),
            entry(List.class, new TypeList()),
            entry(Collection.class, new TypeCollection()),

            entry(Object.class, new TypeObject())

    );


    private RDF4J(final Repository repository) {
        this.repository=repository;
    }

    private RDF4J(final RDF4J rdf4j) {
        this.repository=rdf4j.repository;
        this.context=rdf4j.context;
        this.types=rdf4j.types;
    }


    public RDF4J context(final IRI context) {

        final RDF4J rdf4J=new RDF4J(this);

        rdf4J.context=context;

        return rdf4J;
    }

    public <T> RDF4J type(final Class<T> clazz, final Type<T> codec) {

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        final RDF4J rdf4J=new RDF4J(this);

        final Entry<Class<?>, Type<?>> entry=entry(clazz, codec);

        rdf4J.types=Stream.concat(Stream.of(entry), types.stream()).collect(toList());

        return rdf4J;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RDF4J shape(final Shape shape, final IRI context) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        final Collection<Statement> model=new SHACLCodec().encode(shape).collect(toList());

        try ( final RepositoryConnection connection=repository.getConnection() ) {

            try {

                connection.begin();

                connection.clear(context);
                connection.add(model, context);
                connection.add(this.context != null ? this.context : NIL, SHACL.SHAPES_GRAPH, context, SHACL_SHAPE_GRAPH);
                connection.commit();

            } catch ( final Throwable t ) {

                connection.rollback();

                throw t;

            }

        }

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public <V> Optional<V> retrieve(final V model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return process(model, (frame, base) -> {

            final Shape shape=frame.shape();
            final boolean virtual=shape.virtual();

            try ( final RepositoryConnection connection=repository.getConnection() ) {

                final Reader reader=new Reader(this, base, connection);

                final CompletableFuture<Boolean> id;
                if ( virtual ) { id=completedFuture(true); } else {
                    final Resource resource=iri(frame.id());
                    id=reader.fetcher().fetch(resource);
                }

                final CompletableFuture<Optional<Frame<V>>> future=reader.lookup(Set.of(iri(frame.id())), frame);

                reader.execute();

                return id.thenCombine(future, (present, value) -> value.filter(v -> present)).join();

            }

        });
    }


    @Override public <V> Optional<V> create(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return process(value, (frame, base) -> {

            try ( final RepositoryConnection connection=repository.getConnection() ) {

                final IRI id=iri(frame.id());

                final Stream<Statement> description=new Writer(this, base, connection)

                        .encode(frame)
                        .getValue()

                        .filter(statement -> statement.getSubject().equals(id));

                // !!! batch?

                final boolean present=connection.hasStatement(id, null, null, true, context);

                if ( present ) {

                    return Optional.empty();

                } else {

                    try {

                        connection.begin();
                        connection.add(description::iterator, context);
                        connection.commit();

                        return Optional.of(frame);

                    } catch ( final Throwable e ) {

                        connection.rollback();

                        throw e;

                    }

                }

            }

        });
    }

    @Override public <V> Optional<V> update(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return process(value, (frame, base) -> {

            final IRI id=iri(frame.id());

            try ( final RepositoryConnection connection=repository.getConnection() ) {

                final Stream<Statement> description=new Writer(this, base, connection)

                        .encode(frame)
                        .getValue()

                        .filter(statement -> statement.getSubject().equals(id));

                // !!! batch?

                final boolean present=connection.hasStatement(id, null, null, true, context)
                        || connection.hasStatement(null, null, id, true, context);

                if ( present ) {

                    try {

                        connection.begin();
                        connection.remove(id, null, null, context);
                        connection.add(description::iterator, context);
                        connection.commit();

                        return Optional.of(frame);

                    } catch ( final Throwable e ) {

                        connection.rollback();

                        throw e;

                    }

                } else {

                    return Optional.empty();

                }

            }

        });
    }

    @Override public <V> Optional<V> delete(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return process(value, (frame, base) -> {

            final IRI id=iri(frame.id());

            try ( final RepositoryConnection connection=repository.getConnection() ) {

                // !!! batch?

                final boolean present=connection.hasStatement(id, null, null, true, context)
                        || connection.hasStatement(null, null, id, true, context);

                if ( present ) {

                    try {

                        connection.begin();
                        connection.remove(id, null, null, context);
                        connection.remove((Resource)null, null, id, context);
                        connection.commit();

                        return Optional.of(frame);

                    } catch ( final Throwable e ) {

                        connection.rollback();

                        throw e;

                    }

                } else {

                    return Optional.empty();

                }

            }

        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <V> Optional<V> process(
            final V value, final BiFunction<? super Frame<V>, ? super String, Optional<Frame<V>>> processor
    ) {

        return Optional.of(value)

                .map(Frame::frame)

                .flatMap(frame -> processor.apply(frame, root(frame.id())

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "object id <%s> is not an absolute IRI", frame.id()
                        )))

                ))

                .map(Frame::value);
    }


    //// !!! review ////////////////////////////////////////////////////////////////////////////////////////////////////

     static interface Type<T> {

         public CompletableFuture<Optional<T>> lookup(final Reader reader, final Set<Value> values, final T model);

         // !!! public CompletableFuture<Set<Value>> insert(final Encoder encoder, final T value);
         // !!! public CompletableFuture<Set<Value>> remove(final Encoder encoder, final T value);

         public Entry<Stream<Value>, Stream<Statement>> _encode(final Writer writer, final T value);

     }


    abstract static class Delegate {

        private final RDF4J rdf4j;

        private final String base;
        private final RepositoryConnection connection;

        private final Map<Supplier<? extends SPARQL>, SPARQL> workers=new HashMap<>(); // async workers by factory


        private Delegate(final RDF4J rdf4j, final String base, final RepositoryConnection connection) {

            this.rdf4j=rdf4j;

            this.base=base;
            this.connection=connection;
        }


        public ValueFactory factory() {
            return connection.getValueFactory();
        }


        String relativize(final String iri) {
            return iri == null ? null
                    : iri.startsWith(base) ? iri.substring(base.length()-1)
                    : iri;
        }

        String resolve(final String iri) {
            return iri == null ? null
                    : iri.startsWith("/") ? base+iri.substring(1)
                    : iri;
        }


        @SuppressWarnings("unchecked") <T> Type<T> type(final T model) {
            return rdf4j.types.stream()

                    .filter(entry -> entry.getKey().isAssignableFrom(model.getClass()))
                    .findFirst()
                    .map(Entry::getValue)

                    .map(codec -> (Type<T>)codec)

                    .orElseThrow(() -> new IllegalArgumentException(format(
                            "unsupported value type <%s>", model.getClass().getName()
                    )));
        }

        @SuppressWarnings("unchecked") <T extends SPARQL> T worker(final Supplier<T> factory) { // !!! public

            if ( factory == null ) {
                throw new NullPointerException("null factory");
            }

            return (T)workers.computeIfAbsent(factory, Supplier::get);
        }


        void execute() {

            CompletableFuture<?>[] batch;

            do {

                batch=workers.values().stream()
                        .flatMap(v -> { // !!! private
                            return v.run(connection).stream();
                        })
                        .toArray(CompletableFuture[]::new);

                allOf(batch).join();

            } while ( batch.length > 0 );

        }

    }


    static final class Reader extends Delegate {

        private Reader(final RDF4J rdf4j, final String base, final RepositoryConnection connection) {
            super(rdf4j, base, connection);
        }


        <T> CompletableFuture<Optional<T>> lookup(final Set<Value> values, final T model) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            if ( model == null ) {
                throw new NullPointerException("null model");
            }

            return type(model).lookup(this, values, model);
        }


        SPARQLFetcher fetcher() {
            return worker(SPARQLFetcher::new);
        }

        SPARQLSelector selector() {
            return worker(SPARQLSelector::new);
        }

    }

    static final class Writer extends Delegate {

        private final Map<Object, Collection<Value>> cache=new HashMap<>();


        private Writer(final RDF4J rdf4j, final String base, final RepositoryConnection connection) {
            super(rdf4j, base, connection);
        }


        Entry<Stream<Value>, Stream<Statement>> encode(final Object value) {

            final Collection<Value> cached=cache.get(value);

            if ( cached != null ) {

                return entry(cached.stream(), Stream.empty());

            } else {

                final Entry<Stream<Value>, Stream<Statement>> entry=type(value)._encode(this, value);

                final List<Value> values=entry.getKey().collect(toList());

                cache.put(value, values);

                return entry(values.stream(), entry.getValue());

            }
        }


        SPARQLUpdater updater() {
            return worker(SPARQLUpdater::new);
        }

    }

}
