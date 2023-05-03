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

import com.metreeca.link.Frame;
import com.metreeca.link.rdf4j.RDF4J.Decoder;
import com.metreeca.link.rdf4j.RDF4J.Encoder;
import com.metreeca.link.rdf4j.RDF4J.Type;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.frame;

final class TypeObject implements Type<Object> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Object value) {
        return encoder.encode(frame(value));
    }

    @Override public Optional<Object> decode(final Decoder decoder, final Value value, final Object template) {
        return Optional.of(value)

                .filter(Value::isResource)
                .map(Resource.class::cast)

                .flatMap(resource -> Optional.of(frame(template))
                        .map(frame -> frame.id() == null || resource.isBNode() ? frame : frame.id(resource.stringValue()))
                        .flatMap(frame -> decoder.decode(resource, frame))
                        .map(Frame::value)
                );
    }

}
