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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Map.entry;

final class TypeInteger implements Type<Integer> { // !!! selective BigInteger conversions

    @Override public Map.Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Integer value) {
        return entry(Stream.of(encoder.factory().createLiteral(BigInteger.valueOf(value))), Stream.empty());
    }

    @Override public Optional<Integer> decode(final Decoder decoder, final Value value, final Integer template) {
        return Optional.of(value)

                .filter(Value::isLiteral)
                .map(Literal.class::cast)

                .filter(v -> v.getDatatype().equals(XSD.INTEGER) || v.getDatatype().equals(XSD.INT))
                .map(Literal::intValue);
    }

}
