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

package com.metreeca.rest.rdf4j;


import com.metreeca.rest.Shape;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.stream.Stream;

import static com.metreeca.rest.rdf4j.RDF4J.factory;

final class ShapeCodec {


    Stream<Statement> encode(final Shape shape) {

        return shape.type().stream()

                .map(factory::createIRI)

                .flatMap(type -> {

                    // !!! shape.fields()

                    return Stream.of(
                            factory.createStatement(type, RDF.TYPE, RDFS.CLASS),
                            factory.createStatement(type, RDF.TYPE, SHACL.NODE_SHAPE)
                    );

                });
    }

}
