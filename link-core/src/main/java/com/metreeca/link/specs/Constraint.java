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

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.error;

import static java.lang.String.format;
import static java.text.Normalizer.normalize;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * Filtering constraint.
 */
public abstract class Constraint {

    private static final Pattern WordPattern=Pattern.compile("\\w+");
    private static final Pattern MarkPattern=Pattern.compile("\\p{M}");

    public static String pattern(final CharSequence keywords, final boolean stemming) {

        if ( keywords == null ) {
            throw new NullPointerException("null keywords");
        }

        final StringBuilder builder=new StringBuilder(keywords.length()).append("(?i:.*");

        final String normalized=MarkPattern.matcher(normalize(keywords, Normalizer.Form.NFD)).replaceAll("");
        final Matcher matcher=WordPattern.matcher(normalized);

        while ( matcher.find() ) {
            builder.append("\\b").append(matcher.group()).append(stemming ? "" : "\\b").append(".*");
        }

        return builder.append(")").toString();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
