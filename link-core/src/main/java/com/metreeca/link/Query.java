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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.link.Constraint.any;
import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.error;
import static com.metreeca.link.Frame.frame;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

/**
 * Collection query.
 */
public abstract class Query implements Resource {

    private static final Frame EMPTY=frame();


    public static Query filter(final IRI predicate, final Value... values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return filter(expression(predicate), any(values));
    }

    public static Query filter(final IRI predicate, final Constraint constraint) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        return filter(expression(predicate), constraint);
    }

    public static Query filter(final Expression expression, final Constraint constraint) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        final Map<Expression, Constraint> filter=constraint.empty() ? Map.of() : Map.of(expression, constraint);

        return new Query() {

            @Override public Map<Expression, Constraint> filter() { return filter; }

        };
    }


    public static Query order(final IRI predicate, final int priority) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return order(expression(predicate), priority);
    }

    public static Query order(final Expression expression, final int priority) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        final Map<Expression, Integer> value=(priority == 0) ? Map.of() : Map.of(expression, priority);

        return new Query() {

            @Override public Map<Expression, Integer> order() { return value; }

        };
    }


    public static Query focus(final IRI predicate, final Set<Value> values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return focus(expression(predicate), values);
    }

    public static Query focus(final Expression expression, final Set<Value> values) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        final Map<Expression, Set<Value>> value=values.isEmpty() ? Map.of() : Map.of(expression, values);

        return new Query() {

            @Override public Map<Expression, Set<Value>> focus() { return value; }

        };
    }


    public static Query offset(final int offset) {

        if ( offset < 0 ) {
            throw new IllegalArgumentException("negative offset");
        }

        return new Query() {

            @Override public int offset() { return offset; }

        };
    }

    public static Query limit(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        return new Query() {

            @Override public int limit() { return limit; }

        };
    }


    public static Query query(final Frame model, final Query... queries) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( queries == null || Arrays.stream(queries).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null queries");
        }

        return query(model, asList(queries)); // ;( handle null values
    }

    public static Query query(final Frame model, final Collection<Query> queries) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( queries == null || queries.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null queries");
        }


        final Map<Expression, Constraint> filters=queries.stream()
                .flatMap(query -> query.filter().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        Constraint::and,
                        LinkedHashMap::new
                ));

        final Map<Expression, Integer> sorting=queries.stream()
                .flatMap(query -> query.order().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x.equals(y) ? x : error("conflicting <sorting> <%s> / <%s>", x, y),
                        LinkedHashMap::new
                ));

        final Map<Expression, Set<Value>> pinning=queries.stream()
                .flatMap(query -> query.focus().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x.equals(y) ? x : error("conflicting <pinning> <%s> / <%s>", x, y), // !!! merge
                        LinkedHashMap::new
                ));


        final int offset=queries.stream()
                .map(Query::offset)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <offset> <%s> / <%s>", x, y))
                .orElse(0);

        final int limit=queries.stream()
                .map(Query::limit)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <limit> <%s> / <%s>", x, y))
                .orElse(0);


        return new Query() {

            @Override public Frame model() {
                return model;
            }


            @Override public Map<Expression, Constraint> filter() { return filters; }

            @Override public Map<Expression, Integer> order() { return sorting; }

            @Override public Map<Expression, Set<Value>> focus() { return pinning; }


            @Override public int offset() { return offset; }

            @Override public int limit() { return limit; }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Query() { }


    public Frame model() { return EMPTY; }


    public Map<Expression, Constraint> filter() {
        return Map.of();
    }

    public Map<Expression, Integer> order() { return Map.of(); }

    public Map<Expression, Set<Value>> focus() { return Map.of(); }


    public int offset() {
        return 0;
    }

    public int limit() {
        return 0;
    }


    @Override public String stringValue() {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        return super.toString(); // !!!
    }

}

