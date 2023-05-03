/*
 * Copyright © 2020-2023 EC2U Alliance
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

import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

/**
 * Localized textual content.
 */
public final class Local<T> {

    /**
     * A variant of the {@linkplain Locale#ROOT root locale}used in templates to match any locale.
     */
    public static final Locale Wildcard=new Locale.Builder()
            .setLocale(Locale.ROOT)
            .setExtension(Locale.PRIVATE_USE_EXTENSION, "wildcard")
            .build();


    public static Local<String> local(final String locale, final String value) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return local(locale(locale), value);
    }

    public static Local<String> local(final Locale locale, final String value) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Local<>(Map.of(locale, value));
    }


    public static Local<Set<String>> local(final String locale, final String... values) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null value");
        }

        return local(locale(locale), List.of(values));
    }

    public static Local<Set<String>> local(final Locale locale, final String... values) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null value");
        }

        return local(locale, List.of(values));
    }


    public static Local<Set<String>> local(final String locale, final Collection<String> values) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null value");
        }

        return local(locale(locale), values);
    }

    public static Local<Set<String>> local(final Locale locale, final Collection<String> values) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null value");
        }

        return new Local<>(Map.of(locale, new LinkedHashSet<>(values)));
    }


    @SafeVarargs public static <T> Local<T> local(final Local<T>... locals) {

        if ( locals == null || Arrays.stream(locals).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null locals");
        }

        return local(List.of(locals));
    }

    public static <T> Local<T> local(final Collection<Local<T>> locals) {

        if ( locals == null || locals.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null locals");
        }

        return new Local<>(locals.stream()
                .flatMap(local -> local.values.entrySet().stream())
                .collect(toMap(

                        Map.Entry::getKey,
                        Map.Entry::getValue,

                        (x, y) -> {

                            if ( x.equals(y) ) {
                                return x;
                            } else {

                                throw new IllegalArgumentException(format(
                                        "conflicting localized values <%s> / <%s>", x, y
                                ));
                            }

                        },

                        LinkedHashMap::new

                ))
        );
    }


    private static Locale locale(final String locale) {
        return locale.equals("*") ? Wildcard : Locale.forLanguageTag(locale);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Locale, T> values;


    private Local(final Map<Locale, T> values) { this.values=unmodifiableMap(values); }


    public Map<Locale, T> values() {
        return values;
    }


    public Optional<T> value(final String locale) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        return value(locale(locale));
    }

    public Optional<T> value(final Locale locale) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        return Optional.ofNullable(values.get(locale));
    }


    @Override public boolean equals(final Object object) {
        return this == object || object instanceof Local
                && values.equals(((Local<?>) object).values);
    }

    @Override public int hashCode() {
        return values.hashCode();
    }

    @Override public String toString() {
        return values.toString();
    }

}
