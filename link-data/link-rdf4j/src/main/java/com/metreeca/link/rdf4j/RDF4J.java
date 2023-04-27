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
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.base;

import static java.lang.String.format;
import static java.util.Map.entry;
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

    static final ValueFactory factory=new AbstractValueFactory() { };


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

            entry(String.class, new TypeString()),
            entry(URI.class, new TypeURI()),

            entry(Frame.class, new TypeFrame()),
            entry(List.class, new TypeList()),
            entry(Set.class, new TypeCollection()),

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


    @SuppressWarnings("unchecked")
    private <T> Type<T> type(final T template) {
        return types.stream()

                .filter(entry -> entry.getKey().isAssignableFrom(template.getClass()))
                .findFirst()
                .map(Entry::getValue)

                .map(codec -> ((Type<T>)codec))

                .orElseThrow(() -> new IllegalArgumentException(format(
                        "unsupported value type <%s>", template.getClass().getName()
                )));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RDF4J shape(final Shape shape, final IRI context) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        final Collection<Statement> model=new ShapeCodec().encode(shape).collect(toList());

        try ( final RepositoryConnection connection=repository.getConnection() ) {

            try {

                connection.begin();

                connection.clear(context);
                connection.add(model, context);
                connection.add(this.context != null ? this.context : NIL, SHACL.SHAPES_GRAPH, context,
                        SHACL_SHAPE_GRAPH);
                connection.commit();

            } catch ( final Throwable t ) {

                connection.rollback();

                throw t;

            }

        }

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public <V> Optional<V> retrieve(final V template) {

        if ( template == null ) {
            throw new NullPointerException("null template");
        }

        return process(template, (frame, base) -> {

            final IRI id=iri(frame.id());

            try ( final RepositoryConnection connection=repository.getConnection() ) {
                return new Decoder(this, connection, base).decode(id, frame);
            }

        });
    }


    @Override public <V> Optional<V> create(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return process(value, (frame, base) -> {

            final IRI id=iri(frame.id());

            final Stream<Statement> description=new Encoder(this, base)

                    .encode(frame)
                    .getValue()

                    .filter(statement -> statement.getSubject().equals(id));

            try ( final RepositoryConnection connection=repository.getConnection() ) {

                final boolean present=connection.hasStatement(id, null, null, true, context)
                        || connection.hasStatement(null, null, id, true, context);

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

            final Stream<Statement> description=new Encoder(this, base)

                    .encode(frame)
                    .getValue()

                    .filter(statement -> statement.getSubject().equals(id));

            try ( final RepositoryConnection connection=repository.getConnection() ) {

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

                .flatMap(frame -> processor.apply(frame, base(frame.id())

                        .orElseThrow(() -> new IllegalArgumentException(format(
                                "object id <%s> is not an absolute IRI", frame.id()
                        )))

                ))

                .map(Frame::value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static interface Type<T> {

        public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final T value);

        public Optional<T> decode(final Decoder decoder, final Value value, final T template);

    }


    public static final class Encoder {

        private final String base;
        private final RDF4J rdf4j;


        private Encoder(final RDF4J rdf4j, final String base) {

            this.base=base;
            this.rdf4j=rdf4j;

        }


        public ValueFactory factory() {
            return factory;
        }


        public String resolve(final String iri) {
            return iri == null ? null
                    : iri.startsWith("/") ? base + iri.substring(1)
                    : iri;
        }


        public Entry<Stream<Value>, Stream<Statement>> encode(final Object value) {
            return rdf4j.type(value).encode(this, value);
        }

    }

    public static final class Decoder {

        private final String base;
        private final RDF4J rdf4j;

        private final RepositoryConnection connection;


        private Decoder(final RDF4J rdf4j, final RepositoryConnection connection, final String base) {

            this.base=base;
            this.rdf4j=rdf4j;

            this.connection=connection;
        }


        public RepositoryConnection connection() {
            return connection;
        }


        public String relativize(final String iri) {
            return iri == null ? null
                    : iri.startsWith(base) ? iri.substring(base.length()-1)
                    : iri;
        }


        public <T> Optional<T> decode(final Value value, final T template) {
            return value == null ? Optional.empty() : rdf4j.type(template).decode(this, value, template);
        }

    }

}
