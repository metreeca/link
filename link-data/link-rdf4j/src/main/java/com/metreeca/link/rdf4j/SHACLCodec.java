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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.*;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
final class SHACLCodec {

    Collection<Statement> encode(final Collection<Shape> shapes) {
        return shapes.stream()

                .flatMap(shape -> shape.target().stream().map(target -> merge(

                        Stream.of(
                                field(RDF.TYPE, SHACL.NODE_SHAPE),
                                field(SHACL.TARGET_CLASS, target)
                        ),

                        constraints(shape),
                        properties(shape, true)

                )))

                .flatMap(Frame::stream)
                .collect(toList());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Stream<Field> constraints(final Shape shape) {
        return Stream

                .of(

                        datatype(shape),
                        type(shape),

                        minExclusive(shape),
                        maxExclusive(shape),
                        minInclusive(shape),
                        maxInclusive(shape),

                        minLength(shape),
                        maxLength(shape),
                        pattern(shape),

                        minCount(shape),
                        maxCount(shape),

                        in(shape),
                        hasValue(shape)

                )

                .flatMap(identity());
    }


    private Stream<Field> datatype(final Shape shape) {
        return shape.datatype().stream().flatMap(datatype -> Stream.of(

                datatype.equals(VALUE) ? field(SHACL.NODE_KIND, Optional.empty())

                        : datatype.equals(RESOURCE) ? field(SHACL.NODE_KIND, SHACL.BLANK_NODE_OR_IRI)
                        : datatype.equals(BNODE) ? field(SHACL.NODE_KIND, SHACL.BLANK_NODE)
                        : datatype.equals(IRI) ? field(SHACL.NODE_KIND, SHACL.IRI)
                        : datatype.equals(LITERAL) ? field(SHACL.NODE_KIND, SHACL.LITERAL)

                        : field(SHACL.DATATYPE, datatype)

        ));
    }

    private Stream<Field> type(final Shape shape) {
        return shape.type().stream().flatMap(type -> Stream.of(

                field(SHACL.CLASS, type)

        ));
    }


    private Stream<Field> minExclusive(final Shape shape) {
        return shape.minExclusive().stream().flatMap(limit -> Stream.of(

                field(SHACL.MIN_EXCLUSIVE, limit)

        ));
    }

    private Stream<Field> maxExclusive(final Shape shape) {
        return shape.maxExclusive().stream().flatMap(limit -> Stream.of(

                field(SHACL.MAX_EXCLUSIVE, limit)

        ));
    }

    private Stream<Field> minInclusive(final Shape shape) {
        return shape.minInclusive().stream().flatMap(limit -> Stream.of(

                field(SHACL.MIN_INCLUSIVE, limit)

        ));
    }

    private Stream<Field> maxInclusive(final Shape shape) {
        return shape.maxExclusive().stream().flatMap(limit -> Stream.of(

                field(SHACL.MAX_INCLUSIVE, limit)

        ));
    }


    private Stream<Field> minLength(final Shape shape) {
        return shape.minLength().stream().flatMap(limit -> Stream.of(

                field(SHACL.MIN_LENGTH, literal(integer(limit)))

        ));
    }

    private Stream<Field> maxLength(final Shape shape) {
        return shape.maxLength().stream().flatMap(limit -> Stream.of(

                field(SHACL.MAX_LENGTH, literal(integer(limit)))

        ));
    }

    private Stream<Field> pattern(final Shape shape) {
        return shape.pattern().stream().flatMap(pattern -> Stream.of(

                field(SHACL.PATTERN, literal(pattern))

        ));
    }


    private Stream<Field> minCount(final Shape shape) {
        return shape.minCount().stream().flatMap(limit -> Stream.of(

                field(SHACL.MIN_COUNT, literal(integer(limit)))

        ));
    }

    private Stream<Field> maxCount(final Shape shape) {
        return shape.maxCount().stream().flatMap(limit -> Stream.of(shape.localized()

                ? field(SHACL.UNIQUE_LANG, literal(limit == 1))
                : field(SHACL.MAX_COUNT, literal(integer(limit)))

        ));
    }


    private Stream<Field> in(final Shape shape) {
        return shape.in().stream().flatMap(values -> Stream.of(

                field(SHACL.IN, list(values))

        ));
    }


    private static Stream<Field> hasValue(final Shape shape) {
        return shape.hasValue().stream().flatMap(values -> Stream.of(

                field(SHACL.HAS_VALUE, values)

        ));
    }


    private Stream<Field> properties(final Shape shape, final boolean recursive) {
        return shape.predicates().entrySet().stream().flatMap(e -> {

            final IRI predicate=e.getKey();
            final Shape nested=e.getValue().getValue().get();

            if ( !predicate.equals(ID) && (recursive || nested.composite()) ) {

                return Stream.of(field(SHACL.PROPERTY, merge(

                        Stream.of(
                                field(SHACL.PATH, path(predicate))
                        ),

                        constraints(nested),
                        properties(nested, false)

                )));

            } else {

                return Stream.empty();

            }

        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs
    private Frame merge(final Stream<Field>... fields) {
        return frame(Arrays
                .stream(fields)
                .flatMap(identity())
                .collect(toList())
        );
    }


    private static Value path(final IRI predicate) {
        return forward(predicate) ? predicate
                : frame(field(SHACL.INVERSE_PATH, reverse(predicate)));
    }

    private Value list(final Iterable<Value> values) {

        Value list=RDF.NIL;

        for (final Value value : values) {
            list=frame(
                    field(RDF.FIRST, value),
                    field(RDF.REST, list)
            );
        }

        return list;
    }

}
