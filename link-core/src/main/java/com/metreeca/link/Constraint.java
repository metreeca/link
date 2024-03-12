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

import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Frame.compare;
import static com.metreeca.link.Frame.error;

import static java.text.Normalizer.Form.NFD;
import static java.text.Normalizer.normalize;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;

/**
 * Filtering constraint.
 */
public abstract class Constraint {

    private static final Pattern WORD_PATTERN=Pattern.compile("\\w+");
    private static final Pattern MARK_PATTERN=Pattern.compile("\\p{M}");


    public static String pattern(final CharSequence keywords, final boolean stemming) {

        if ( keywords == null ) {
            throw new NullPointerException("null keywords");
        }

        final StringBuilder builder=new StringBuilder(keywords.length()).append("(?i:.*");

        final String normalized=MARK_PATTERN.matcher(normalize(keywords, NFD)).replaceAll("");
        final Matcher matcher=WORD_PATTERN.matcher(normalized);

        while ( matcher.find() ) {
            builder.append("\\b").append(matcher.group()).append(stemming ? "" : "\\b").append(".*");
        }

        return builder.append(")").toString();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Constraint lt(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> lt=Optional.of(limit);

        return new Constraint() {

            @Override public Optional<Value> lt() { return lt; }

        };

    }

    public static Constraint gt(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> gt=Optional.of(limit);

        return new Constraint() {

            @Override public Optional<Value> gt() { return gt; }

        };

    }

    public static Constraint lte(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> lte=Optional.of(limit);

        return new Constraint() {

            @Override public Optional<Value> lte() { return lte; }

        };

    }

    public static Constraint gte(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        final Optional<Value> gte=Optional.of(limit);

        return new Constraint() {

            @Override public Optional<Value> gte() { return gte; }

        };

    }


    public static Constraint like(final String keywords) {

        if ( keywords == null ) {
            throw new NullPointerException("null keywords");
        }

        final Optional<String> like=Optional.of(keywords).filter(not(String::isBlank));

        return new Constraint() {

            @Override public Optional<String> like() { return like; }

        };

    }


    public static Constraint any(final Value... values) {

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return any(asList(values));
    }

    public static Constraint any(final Collection<? extends Value> values) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        final Optional<Set<Value>> any=Optional.of(Set.copyOf(values));

        return new Constraint() {

            @Override public Optional<Set<Value>> any() { return any; }

        };

    }


    public static Constraint and(final Constraint... constraints) {

        if ( constraints == null || Arrays.stream(constraints).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null constraints");
        }

        return and(asList(constraints));
    }

    public static Constraint and(final Collection<Constraint> constraints) {

        if ( constraints == null || constraints.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null constraints");
        }


        final Optional<Value> lt=constraints.stream().flatMap(c -> c.lt().stream()).reduce((x, y) ->
                compare(x, y) <= 0 ? x : y
        );

        final Optional<Value> gt=constraints.stream().flatMap(c -> c.gt().stream()).reduce((x, y) ->
                compare(x, y) >= 0 ? x : y

        );

        final Optional<Value> lte=constraints.stream().flatMap(c -> c.lte().stream()).reduce((x, y) ->
                compare(x, y) <= 0 ? x : y
        );

        final Optional<Value> gte=constraints.stream().flatMap(c -> c.gte().stream()).reduce((x, y) ->
                compare(x, y) >= 0 ? x : y
        );


        final Optional<String> like=constraints.stream().flatMap(v -> v.like().stream()).reduce((x, y) ->
                x.equals(y) ? x : error("conflicting <like> constraints <%s> / <%s>", x, y)
        );

        final Optional<Set<Value>> any=constraints.stream().flatMap(v -> v.any().stream()).reduce((x, y) ->
                x.containsAll(y) ? y : y.containsAll(x) ? x : error("conflicting <any> constraints <%s> / <%s>", x, y)
        );


        if ( lt.isPresent() && lte.isPresent() ) {
            error("conflicting <lt/lte> constraints <%s> / <%s>", lt.get(), lte.get());
        }

        if ( gt.isPresent() && gte.isPresent() ) {
            error("conflicting <gt/gte> constraints <%s> / <%s>", gt.get(), gte.get());
        }


        return new Constraint() {

            @Override public Optional<Value> lt() { return lt; }

            @Override public Optional<Value> gt() { return gt; }


            @Override public Optional<Value> lte() { return lte; }

            @Override public Optional<Value> gte() { return gte; }


            @Override public Optional<String> like() { return like; }

            @Override public Optional<Set<Value>> any() { return any; }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Constraint() { }


    public boolean empty() {
        return lt().isEmpty()
                && gt().isEmpty()
                && lte().isEmpty()
                && gte().isEmpty()

                && like().isEmpty()

                && any().isEmpty();
    }


    public Optional<Value> lt() {
        return Optional.empty();
    }

    public Optional<Value> gt() {
        return Optional.empty();
    }

    public Optional<Value> lte() {
        return Optional.empty();
    }

    public Optional<Value> gte() {
        return Optional.empty();
    }


    public Optional<String> like() {
        return Optional.empty();
    }


    public Optional<Set<Value>> any() {
        return Optional.empty();
    }

}
