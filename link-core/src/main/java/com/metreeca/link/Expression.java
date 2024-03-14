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

package com.metreeca.link;

import org.eclipse.rdf4j.model.IRI;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;

/**
 * Value expression.
 */
public final class Expression implements Serializable {

    private static final long serialVersionUID=3791554565141703741L;


    public static Expression expression(final IRI... path) {

        if ( path == null || Arrays.stream(path).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null path");
        }

        return new Expression(List.of(), List.of(path));
    }

    public static Expression expression(final List<Transform> pipe, final List<IRI> path) {

        if ( pipe == null || pipe.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null pipe");
        }

        if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null path");
        }

        return new Expression(pipe, path);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<Transform> pipe;
    private final List<IRI> path;


    private Expression(final List<Transform> pipe, final List<IRI> path) {
        this.pipe=unmodifiableList(pipe);
        this.path=unmodifiableList(path);
    }


    public boolean computed() {
        return !pipe.isEmpty();
    }

    public boolean aggregate() {
        return pipe.stream().anyMatch(Transform::aggregate);
    }


    public List<Transform> pipe() {
        return pipe;
    }

    public List<IRI> path() {
        return path;
    }


    public Shape apply(final Shape shape) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        Shape mapped=shape;

        for (final IRI step : path()) {
            mapped=mapped.entry(step)
                    .orElseThrow(() -> new IllegalArgumentException(format("unknown property predicate <%s>", step)))
                    .getValue();
        }


        for (final Transform transform : pipe()) {
            mapped=transform.apply(mapped);
        }

        return mapped;
    }


    @Override public boolean equals(final Object object) {
        return this == object || object instanceof Expression
                && pipe.equals(((Expression)object).pipe)
                && path.equals(((Expression)object).path);
    }

    @Override public int hashCode() {
        return pipe.hashCode()
                ^path.hashCode();
    }

    @Override public String toString() {
        return Stream

                .concat(

                        pipe.stream()
                                .map(transform -> format("%s:", transform.name().toLowerCase(ROOT))),

                        path.stream()
                                .map(IRI::getLocalName)
                                .map(step -> format("%s", step))

                )

                .collect(joining());
    }

}
