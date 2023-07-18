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

import com.metreeca.link.rdf4j.RDF4J.Reader;
import com.metreeca.link.rdf4j.RDF4J.Type;
import com.metreeca.link.rdf4j.RDF4J.Writer;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static java.util.concurrent.CompletableFuture.completedFuture;

final class TypeBigInteger implements Type<BigInteger> {

    @Override public CompletableFuture<Optional<BigInteger>> lookup(final Reader reader, final Set<Value> values, final BigInteger model) {
        return completedFuture(values.stream()

                .filter(Value::isLiteral)
                .map(Literal.class::cast)

                .findFirst()

                .map(Literal::integerValue)

        );
    }

    @Override public Map.Entry<Stream<Value>, Stream<Statement>> _encode(final Writer writer, final BigInteger value) {
        return entry(Stream.of(writer.factory().createLiteral(value)), Stream.empty());
    }

}
