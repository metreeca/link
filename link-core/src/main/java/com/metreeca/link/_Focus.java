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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.forward;
import static com.metreeca.link.Frame.reverse;

import static java.util.stream.Collectors.toCollection;

final class _Focus {

    public static _Focus focus(final Value value, final Collection<Statement> statements) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( statements == null || statements.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null statements");
        }

        return new _Focus(Set.of(value), new LinkedHashSet<>(statements));
    }

    public static _Focus focus(final Collection<Value> values, final Collection<Statement> statements) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        if ( statements == null || statements.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null statements");
        }

        return new _Focus(new LinkedHashSet<>(values), new LinkedHashSet<>(statements));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Set<Value> values;
    private final Set<Statement> statements;


    private _Focus(final Set<Value> values, final Set<Statement> statements) {
        this.values=values;
        this.statements=statements;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean empty() {
        return values.isEmpty();
    }


    public Optional<Value> value() { return values().findFirst(); }

    public Stream<Value> values() {
        return values.stream();
    }


    public <T> Optional<T> value(final Function<Value, T> converter) {

        if ( converter == null ) {
            throw new NullPointerException("null converter");
        }

        return value().map(guard(converter));
    }

    public <T> Stream<T> values(final Function<Value, T> converter) {

        if ( converter == null ) {
            throw new NullPointerException("null converter");
        }
        return values().map(guard(converter)).filter(Objects::nonNull);
    }


    public _Focus shift(final IRI step) {

        if ( step == null ) {
            throw new NullPointerException("null step");
        }

        return new _Focus(shift(values, step), statements);
    }

    public _Focus shift(final IRI... steps) {

        if ( steps == null || Arrays.stream(steps).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null steps");
        }

        Set<Value> next=values;

        for (final IRI step : steps) { next=shift(next, step); }

        return new _Focus(next, statements);
    }


    public Stream<_Focus> split() {
        return values.stream().map(value -> new _Focus(Set.of(value), statements));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <T> Function<Value, T> guard(final Function<Value, T> converter) {
        return value -> {

            try {

                return converter.apply(value);

            } catch ( final RuntimeException ignored ) {

                return null;

            }

        };
    }


    private Set<Value> shift(final Collection<Value> values, final IRI step) {
        return forward(step)
                ? recto(values, step)
                : verso(values, reverse(step));
    }

    private Set<Value> verso(final Collection<Value> values, final IRI reverse) {
        return statements.stream()
                .filter(statement -> reverse.equals(statement.getPredicate()))
                .filter(statement -> values.contains(statement.getObject()))
                .map(Statement::getSubject)
                .collect(toCollection(LinkedHashSet::new));
    }

    private Set<Value> recto(final Collection<Value> values, final IRI step) {
        return statements.stream()
                .filter(statement -> step.equals(statement.getPredicate()))
                .filter(statement -> values.contains(statement.getSubject()))
                .map(Statement::getObject)
                .collect(toCollection(LinkedHashSet::new));
    }

}
