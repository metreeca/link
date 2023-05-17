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

package com.metreeca.link;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Normalizer.Form;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.link.Glass.error;
import static com.metreeca.link.Stash.Expression.expression;

import static java.lang.String.format;
import static java.text.Normalizer.normalize;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

/**
 * Collection query.
 */
public abstract class Query<T> extends Stash<T> {

    private static final Pattern WordPattern=Pattern.compile("\\w+");
    private static final Pattern MarkPattern=Pattern.compile("\\p{M}");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> Query<T> query(final Query<?>... queries) {

        if ( queries == null ) {
            throw new NullPointerException("null queries");
        }

        return query(asList(queries)); // ;( handle null values
    }

    public static <T> Query<T> query(final Collection<Query<?>> queries) {

        if ( queries == null ) {
            throw new NullPointerException("null queries");
        }

        final Object model=queries.stream()
                .map(query -> query.model())
                .filter(Objects::nonNull)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <model> <%s> / <%s>", x, y))
                .orElse(null);

        final Map<Expression, Constraint> facets=queries.stream()
                .flatMap(query -> query.filters().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        Constraint::and,
                        LinkedHashMap::new
                ));

        final Map<Expression, Criterion> order=queries.stream()
                .flatMap(query -> query.order().entrySet().stream())
                .collect(toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x, y) -> x == y ? x : error("conflicting <order> <%s> / <%s>", x, y),
                        LinkedHashMap::new
                ));

        final int offset=queries.stream().map(Query::offset)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <offset> constraints <%s> / <%s>", x, y))
                .orElse(0);

        final int limit=queries.stream().map(Query::limit)
                .filter(value -> value > 0)
                .reduce((x, y) -> x.equals(y) ? x : error("conflicting <limit> constraints <%s> / <%s>", x, y))
                .orElse(0);


        return new Query<>() {

            @Override public Object model() { return model; }

            @Override public Map<Expression, Constraint> filters() {
                return facets;
            }

            @Override public int offset() {
                return offset;
            }

            @Override public int limit() {
                return limit;
            }

            @Override public Map<Expression, Criterion> order() { return order; }

        };
    }


    public static <T> Query<T> model(final T model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Query<>() {

            @Override public Object model() { return model; }

        };
    }


    public static <T> Query<T> filter(final String expression, final Constraint constraint) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        return filter(expression(expression), constraint);
    }

    public static <T> Query<T> filter(final Expression expression, final Constraint constraint) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( constraint == null ) {
            throw new NullPointerException("null constraint");
        }

        final Map<Expression, Constraint> facets=Map.of(expression, constraint);

        return new Query<>() {

            @Override public Map<Expression, Constraint> filters() { return facets; }

        };
    }


    public static <T> Query<T> order(final String expression, final Criterion criterion) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( criterion == null ) {
            throw new NullPointerException("null criterion");
        }

        return order(expression(expression), criterion);
    }

    public static <T> Query<T> order(final Expression expression, final Criterion criterion) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( criterion == null ) {
            throw new NullPointerException("null criterion");
        }

        final Map<Expression, Criterion> order=Map.of(expression, criterion);

        return new Query<>() {

            @Override public Map<Expression, Criterion> order() { return order; }

        };
    }

    public static <T> Query<T> offset(final int offset) {

        if ( offset < 0 ) {
            throw new IllegalArgumentException("negative offset");
        }

        return new Query<>() {

            @Override public int offset() { return offset; }

        };
    }

    public static <T> Query<T> limit(final int limit) {

        if ( limit < 0 ) {
            throw new IllegalArgumentException("negative limit");
        }

        return new Query<>() {

            @Override public int limit() { return limit; }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String pattern(final CharSequence keywords, final boolean stemming) {

        if ( keywords == null ) {
            throw new NullPointerException("null keywords");
        }

        final StringBuilder builder=new StringBuilder(keywords.length()).append("(?i:.*");

        final String normalized=MarkPattern.matcher(normalize(keywords, Form.NFD)).replaceAll("");
        final Matcher matcher=WordPattern.matcher(normalized);

        while ( matcher.find() ) {
            builder.append("\\b").append(matcher.group()).append(stemming ? "" : "\\b").append(".*");
        }

        return builder.append(")").toString();
    }


    public static BigInteger integer(final long value) {
        return BigInteger.valueOf(value);
    }

    public static BigDecimal decimal(final double value) {
        return BigDecimal.valueOf(value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Query() { }


    /**
     * Retrieves the query model.
     *
     * @return the possibly null query model
     */
    public Object model() { return null; }


    public Map<Expression, Constraint> filters() {
        return Map.of();
    }


    public Map<Expression, Criterion> order() { return Map.of(); }

    public int offset() {
        return 0;
    }

    public int limit() {
        return 0;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Filtering constraint.
     */
    public abstract static class Constraint {

        public static Constraint and(final Constraint... constraints) {

            if ( constraints == null || Arrays.stream(constraints).anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null constraints");
            }

            return and(asList(constraints));
        }

        @SuppressWarnings("unchecked")
        public static Constraint and(final Collection<Constraint> constraints) {

            if ( constraints == null || constraints.stream().anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null constraints");
            }


            final Optional<Object> lt=constraints.stream().flatMap(c -> c.lt().stream()).reduce((x, y) ->
                    x.equals(y) ? x
                            : comparable(x, y) ? ((Comparable<Object>)x).compareTo(y) <= 0 ? x : y
                            : error("conflicting <lt> constraints <%s> / <%s>", x, y)
            );

            final Optional<Object> gt=constraints.stream().flatMap(c -> c.gt().stream()).reduce((x, y) ->
                    x.equals(y) ? x
                            : comparable(x, y) ? ((Comparable<Object>)x).compareTo(y) >= 0 ? x : y
                            : error("conflicting <gt> constraints <%s> / <%s>", x, y)
            );


            final Optional<Object> lte=constraints.stream().flatMap(c -> c.lte().stream()).reduce((x, y) ->
                    x.equals(y) ? x
                            : comparable(x, y) ? ((Comparable<Object>)x).compareTo(y) <= 0 ? x : y
                            : error("conflicting <lte> constraints <%s> / <%s>", x, y)
            );

            final Optional<Object> gte=constraints.stream().flatMap(c -> c.gte().stream()).reduce((x, y) ->
                    x.equals(y) ? x
                            : comparable(x, y) ? ((Comparable<Object>)x).compareTo(y) >= 0 ? x : y
                            : error("conflicting <gte> constraints <%s> / <%s>", x, y)
            );


            final Set<String> like=constraints.stream().flatMap(c -> c.like().stream()).collect(toSet());

            final Set<Set<Object>> any=constraints.stream().flatMap(c -> c.any().stream()).collect(toSet());


            if ( lt.isPresent() && lte.isPresent() ) {
                error("conflicting <lt/lte> constraints <%s> / <%s>", lt.get(), lte.get());
            }

            if ( gt.isPresent() && gte.isPresent() ) {
                error("conflicting <gt/gte> constraints <%s> / <%s>", gt.get(), gte.get());
            }


            return new Constraint() {

                @Override public Optional<Object> lt() { return lt; }

                @Override public Optional<Object> gt() { return gt; }


                @Override public Optional<Object> lte() { return lte; }

                @Override public Optional<Object> gte() { return gte; }


                @Override public Set<String> like() { return like; }

                @Override public Set<Set<Object>> any() { return any; }

            };
        }


        private static boolean comparable(final Object x, final Object y) {
            return x instanceof Comparable && x.getClass().equals(y.getClass());
        }


        public static Constraint lt(final Object limit) {

            if ( limit == null ) {
                throw new NullPointerException("null limit");
            }

            final Optional<Object> lt=Optional.of(limit);

            return new Constraint() {

                @Override public Optional<Object> lt() { return lt; }

            };

        }

        public static Constraint gt(final Object limit) {

            if ( limit == null ) {
                throw new NullPointerException("null limit");
            }

            final Optional<Object> gt=Optional.of(limit);

            return new Constraint() {

                @Override public Optional<Object> gt() { return gt; }

            };

        }

        public static Constraint lte(final Object limit) {

            if ( limit == null ) {
                throw new NullPointerException("null limit");
            }

            final Optional<Object> lte=Optional.of(limit);

            return new Constraint() {

                @Override public Optional<Object> lte() { return lte; }

            };

        }

        public static Constraint gte(final Object limit) {

            if ( limit == null ) {
                throw new NullPointerException("null limit");
            }

            final Optional<Object> gte=Optional.of(limit);

            return new Constraint() {

                @Override public Optional<Object> gte() { return gte; }

            };

        }


        public static Constraint like(final String... keywords) {

            if ( keywords == null || Arrays.stream(keywords).anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null keywords");
            }

            return like(asList(keywords));
        }

        public static Constraint like(final Collection<String> keywords) {

            if ( keywords == null || keywords.stream().anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null keywords");
            }

            final Set<String> like=keywords.stream()
                    .map(String::trim)
                    .filter(not(String::isBlank))
                    .collect(toSet());

            return new Constraint() {

                @Override public Set<String> like() { return like; }

            };

        }


        public static Constraint any(final Object... values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return any(asList(values));
        }

        public static Constraint any(final Collection<?> values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            final Set<Set<Object>> any=Set.of(new LinkedHashSet<>(values)); // ;( handle null values

            return new Constraint() {

                @Override public Set<Set<Object>> any() { return any; }

            };

        }


        private Constraint() { }


        public Optional<Object> lt() {
            return Optional.empty();
        }

        public Optional<Object> gt() {
            return Optional.empty();
        }

        public Optional<Object> lte() {
            return Optional.empty();
        }

        public Optional<Object> gte() {
            return Optional.empty();
        }


        public Set<String> like() {
            return Set.of();
        }

        public Set<Set<Object>> any() {
            return Set.of();
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Constraint
                    && lt().equals(((Constraint)object).lt())
                    && gt().equals(((Constraint)object).gt())
                    && lte().equals(((Constraint)object).lte())
                    && gte().equals(((Constraint)object).gte())
                    && like().equals(((Constraint)object).like())
                    && any().equals(((Constraint)object).any());
        }

        @Override public int hashCode() {
            return lt().hashCode()
                    ^gt().hashCode()
                    ^lte().hashCode()
                    ^gte().hashCode()
                    ^like().hashCode()
                    ^any().hashCode();
        }

        @Override public String toString() {
            return Stream

                    .of(

                            lt().map(limit -> format("< <%s>", limit)),
                            gt().map(limit -> format("> <%s>", limit)),
                            lte().map(limit -> format("<= <%s>", limit)),
                            gte().map(limit -> format(">= <%s>", limit)),

                            like().isEmpty() ? Optional.<String>empty() : Optional.of(like().stream()
                                    .map(v -> format("<%s>", v))
                                    .collect(joining(", ", "~ {", "}"))
                            ),

                            any().isEmpty() ? Optional.<String>empty() : Optional.of(any().stream()
                                    .map(s -> s.stream()
                                            .map(v -> format("<%s>", v))
                                            .collect(joining(", ", "{", "}"))
                                    )
                                    .collect(joining("", "? {", "}"))
                            )

                    )

                    .flatMap(Optional::stream)
                    .collect(joining(" & ", "{ ", " }"));
        }

    }


    /**
     * Sorting criterion.
     */
    public static final class Criterion {

        private final boolean inverse;

        private final Set<Object> values;


        public static Criterion increasing(final Object... values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return new Criterion(false, Set.of(values));
        }


        public static Criterion increasing(final Collection<?> values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return new Criterion(false, values);
        }


        public static Criterion decreasing(final Object... values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return new Criterion(true, Set.of(values));
        }

        public static Criterion decreasing(final Collection<?> values) {

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return new Criterion(true, values);
        }


        private Criterion(final boolean inverse, final Collection<?> values) {
            this.inverse=inverse;
            this.values=Set.copyOf(values);
        }

        public boolean inverse() {
            return inverse;
        }

        public Set<Object> values() {
            return values;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Criterion
                    && inverse == ((Criterion)object).inverse
                    && values.equals(((Criterion)object).values);
        }

        @Override public int hashCode() {
            return Boolean.hashCode(inverse)
                    ^values.hashCode();
        }

        @Override public String toString() {
            return values.stream()
                    .map(v -> format("<%s>", v))
                    .collect(joining(", ", inverse ? "decreasing(" : "increasing(", ")"));
        }

    }

}

