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

package com.metreeca.link.json;

import com.metreeca.link.*;

import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static com.metreeca.link.Frame.field;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.json._Expression._query;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;

/**
 * JSON codec.
 */
public final class JSON implements Codec {

    public static JSON json() {
        return new JSON();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean pretty;


    private JSON() { }

    private JSON(final JSON json) {
        this.pretty=json.pretty;
    }


    public boolean pretty() {
        return pretty;
    }

    public JSON pretty(final boolean pretty) {

        final JSON json=new JSON(this);

        json.pretty=pretty;

        return json;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Frame decode(final String string, final Shape shape) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( string.startsWith("{") ) {

            try ( final StringReader reader=new StringReader(string) ) {

                return decode(reader, shape);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }

        } else if ( string.startsWith("%7B") ) { // URLEncoded JSON

            return decode(URLDecoder.decode(string, UTF_8), shape);

        } else if ( string.startsWith("e3") ) { // Base64 JSON

            return decode(new String(Base64.getDecoder().decode(string), UTF_8), shape);

        } else { // search parameters

            final List<Entry<IRI, Shape>> collections=shape.predicates().entrySet().stream()

                    .map(e -> entry(e.getKey(), e.getValue().getValue().get()))

                    .filter(e -> e.getValue().maxCount().orElse(Integer.MAX_VALUE) > 1)
                    .filter(e -> !e.getValue().predicates().isEmpty())

                    .collect(toList());

            if ( collections.size() == 1 ) {

                try {

                    final Entry<IRI, Shape> collection=collections.iterator().next();

                    final IRI predicate=collection.getKey();
                    final Query query=_query(string, collection.getValue());

                    return frame(field(predicate, query));

                } catch ( final RuntimeException e ) { // !!! review

                    throw new JSONException(e.getMessage(), 1, 1);

                }

            } else if ( collections.isEmpty() ) {

                throw new IllegalArgumentException("no collection property found");

            } else {

                throw new IllegalArgumentException("multiple collection properties found");

            }

        }

    }

    @Override
    public Frame decode(final Readable source, final Shape shape) throws IOException {

        if ( source == null ) {
            throw new NullPointerException("null source");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return execute(() -> new JSONDecoder(this, source).decode(shape));
    }


    @Override
    public <A extends Appendable> A encode(final A target, final Shape shape, final Frame frame) throws IOException {

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( frame == null ) {
            throw new NullPointerException("null frame");
        }

        return execute(() -> {

            final JSONEncoder encoder=new JSONEncoder(this, target);

            encoder.encode(shape, frame);

            return target;

        });
    }

    @Override
    public <A extends Appendable> A encode(final A target, final Shape shape, final Trace trace) throws IOException {

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( trace == null ) {
            throw new NullPointerException("null trace");
        }

        return execute(() -> {

            final JSONEncoder encoder=new JSONEncoder(this, target);

            encoder.encode(shape, trace);

            return target;

        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <T> T execute(final Supplier<T> task) throws IOException {
        try {

            return task.get();

        } catch ( final UncheckedIOException e ) {

            throw e.getCause();

        }
    }

}
