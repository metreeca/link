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

package com.metreeca.link.specs;

import java.util.*;

import static com.metreeca.link.Frame.error;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

/**
 * Collection fetching specs.
 */
public abstract class Specs {

    public static Specs specs(final Specs... specs) {

        if ( specs == null || Arrays.stream(specs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null specs");
        }

        return specs(asList(specs)); // ;( handle null values
    }

    public static Specs specs(final Collection<Specs> specs) {

        if ( specs == null || specs.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null specs");
        }


        final Map<Expression, Constraint> filter=specs.stream()
                .flatMap(query -> query.filter().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        Constraint::and,
                        LinkedHashMap::new
                ));

        final Map<Expression, Criterion> order=specs.stream()
                .flatMap(query -> query.order().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x == y ? x : error("conflicting <order> <%s> / <%s>", x, y),
                        LinkedHashMap::new
                ));

        final Map<Expression, Set<Object>> focus=specs.stream()
                .flatMap(query -> query.focus().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x.equals(y) ? x : error("conflicting <focus> <%s> / <%s>", x, y),
                        LinkedHashMap::new
                ));


        final int offset=specs.stream().map(Specs::offset)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <offset> constraints <%s> / <%s>", x, y))
                .orElse(0);

        final int limit=specs.stream().map(Specs::limit)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <limit> constraints <%s> / <%s>", x, y))
                .orElse(0);


        return new Specs() {

            @Override public Map<Expression, Constraint> filter() {
                return filter;
            }

            @Override public Map<Expression, Criterion> order() { return order; }

            @Override public Map<Expression, Set<Object>> focus() { return focus; }


            @Override public int offset() {
                return offset;
            }

            @Override public int limit() {
                return limit;
            }

        };
    }


    public static Specs filter(final String expression, final Constraint constraint) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        return filter(Expression.expression(expression), constraint);
    }

    public static Specs filter(final Expression expression, final Constraint constraint) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        final Map<Expression, Constraint> filter=Map.of(expression, constraint);

        return new Specs() {

            @Override public Map<Expression, Constraint> filter() { return filter; }

        };
    }


    public static Specs order(final String expression, final Criterion criterion) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( criterion == null ) {
            throw new NullPointerException("null criterion");
        }

        return order(Expression.expression(expression), criterion);
    }

    public static Specs order(final Expression expression, final Criterion criterion) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( criterion == null ) {
            throw new NullPointerException("null criterion");
        }

        final Map<Expression, Criterion> order=Map.of(expression, criterion);

        return new Specs() {

            @Override public Map<Expression, Criterion> order() { return order; }

        };
    }


    public static Specs focus(final String expression, final Set<Object> values) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return focus(Expression.expression(expression), values);
    }

    public static Specs focus(final Expression expression, final Set<Object> values) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        final Map<Expression, Set<Object>> focus=values.isEmpty() ? Map.of() : Map.of(expression, values);

        return new Specs() {

            @Override public Map<Expression, Set<Object>> focus() { return focus; }

        };
    }


    public static Specs offset(final int offset) {

        if ( offset < 0 ) {
            throw new IllegalArgumentException("negative offset");
        }

        return new Specs() {

            @Override public int offset() { return offset; }

        };
    }

    public static Specs limit(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        return new Specs() {

            @Override public int limit() { return limit; }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Specs() { }


    public Map<Expression, Constraint> filter() {
        return Map.of();
    }

    public Map<Expression, Criterion> order() { return Map.of(); }

    public Map<Expression, Set<Object>> focus() { return Map.of(); }


    public int offset() {
        return 0;
    }

    public int limit() {
        return 0;
    }

}

