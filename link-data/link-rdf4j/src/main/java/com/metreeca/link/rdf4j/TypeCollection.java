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

import com.metreeca.link.rdf4j.RDF4J.*;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Map.entry;

final class TypeCollection implements Type<Set<?>> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Set<?> value) {
        return value.stream().map(encoder::encode).reduce(entry(Stream.empty(), Stream.empty()), (x, y) -> entry(
                Stream.concat(x.getKey(), y.getKey()),
                Stream.concat(x.getValue(), y.getValue())
        ));
    }

    @Override public Optional<Set<?>> decode(final Decoder decoder, final Value value, final Set<?> template) {
        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }

}