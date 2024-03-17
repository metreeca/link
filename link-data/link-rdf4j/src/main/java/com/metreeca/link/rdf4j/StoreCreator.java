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

package com.metreeca.link.rdf4j;

import com.metreeca.link.Frame;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.stream.Stream;

import static com.metreeca.link.Frame.ID;
import static com.metreeca.link.Frame.error;

import static java.util.function.Predicate.not;

final class StoreCreator {

    private final RDF4J.Context context;


    StoreCreator(final RDF4J.Context context) {
        this.context=context;
    }


    boolean create(final IRI id, final Shape shape, final Frame frame) {

        final RepositoryConnection connection=context.connection();

        if ( connection.hasStatement(id, null, null, true) ) { // !!! context

            return false;

        } else {

            final Stream<Statement> description=encode(shape, id, frame, connection.getValueFactory());

            try {

                connection.begin();
                connection.add(description::iterator); // !!! context // !!! use SPARQLUpdater
                connection.commit();

                return true;

            } catch ( final Throwable e ) {

                connection.rollback();

                throw e;

            }

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Stream<Statement> encode(final Shape shape, final Resource id, final Frame frame, final ValueFactory factory) {

        return shape.predicates().keySet().stream()

                .filter(not(ID::equals))

                .flatMap(property -> frame.values(property)

                        .map(value -> {

                            if ( value instanceof Frame ) {

                                final Frame _frame=(Frame)value;
                                final Resource _id=_frame.id().map(Resource.class::cast).orElseGet(factory::createBNode);

                                return link(id, property, _id, factory);

                                // !!! cascading return Stream.concat(link(id, property, _id, factory), encode(_id, _frame, factory));

                            } else {

                                return link(id, property, value, factory);

                            }

                        })

                );

    }

    private Statement link(final Resource id, final IRI property, final Value value, final ValueFactory factory) {
        return Frame.forward(property) ? (factory.createStatement(id, property, value))
                : value.isResource() ? (factory.createStatement((Resource)value, Frame.reverse(property), id))
                : error("value <%s> for reverse predicate <%s> is not a resource", value, property);
    }

}
