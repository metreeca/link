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

import com.metreeca.link.rdf4j.RDF4J.Type;
import com.metreeca.link.rdf4j.RDF4J.Writer;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Map.entry;

final class TypeCollection implements Type<Collection<?>> {

    @Override public CompletableFuture<Optional<Collection<?>>> lookup(final RDF4J.Reader reader, final Set<Value> values, final Collection<?> model) {

        throw new UnsupportedOperationException(";( be implemented"); // !!!

        // final RepositoryConnection connection=context.connection();
        //
        // final TypeFrameGenerator generator=new TypeFrameGenerator();
        //
        // final String members=query(generator.members(
        //         resource,
        //         shape.virtual() ? Optional.empty() : Optional.of(property),
        //         subshape,
        //         Query.query()
        // ));
        //
        // final List<Value> values=select(connection, members, bindings ->
        //         bindings.getValue(generator.id())
        // );
        //
        // return values.stream() // !!! batch retrieval
        //         .flatMap(v -> collection.stream()
        //                 .flatMap(o -> context.lookup(v, o).stream())
        //         )
        //         .collect(toList());
    }

    @Override public Entry<Stream<Value>, Stream<Statement>> _encode(final Writer writer, final Collection<?> value) {
        return value.stream().map(writer::encode).reduce(entry(Stream.empty(), Stream.empty()), (x, y) -> entry(
                Stream.concat(x.getKey(), y.getKey()),
                Stream.concat(x.getValue(), y.getValue())
        ));
    }

}
