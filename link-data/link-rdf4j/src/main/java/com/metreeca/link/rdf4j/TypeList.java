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

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class TypeList implements Type<List<?>> {

    @Override public CompletableFuture<Optional<List<?>>> lookup(
            final RDF4J.Reader reader, final Set<Value> values, final List<?> model
    ) {
        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }

    @Override public Entry<Stream<Value>, Stream<Statement>> _encode(
            final Writer writer, final List<?> value
    ) {
        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }

}
