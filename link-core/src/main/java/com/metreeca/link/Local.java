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

import static com.metreeca.link.Frame.error;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

/**
 * Localized textual content.
 */
public final class Local<T> implements Map<Locale, T> {

    /**
     * A wildcard language tag used in models to match any locale.
     */
    public static final String Any="*";

    /**
     * A wildcard variant of the {@linkplain Locale#ROOT root locale} used in models to match any locale.
     */
    public static final Locale AnyLocale=new Locale.Builder()
            .setLocale(Locale.ROOT)
            .setExtension(Locale.PRIVATE_USE_EXTENSION, "any")
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
                .flatMap(local -> local.delegate.entrySet().stream())
                .collect(toMap(

                        Map.Entry::getKey,
                        Map.Entry::getValue,

                        (x, y) -> x.equals(y) ? x : error(
                                "conflicting localized values <%s> / <%s>", x, y
                        ),

                        LinkedHashMap::new

                ))
        );
    }


    private static Locale locale(final String locale) {
        return locale.equals(Any) ? AnyLocale : Locale.forLanguageTag(locale);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Locale, T> delegate;


    private Local(final Map<Locale, T> delegate) { this.delegate=unmodifiableMap(delegate); }


    public Optional<T> get(final String locale) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        return get(locale(locale));
    }

    public Optional<T> get(final Locale locale) {

        if ( locale == null ) {
            throw new NullPointerException("null locale");
        }

        return Optional.ofNullable(delegate.get(locale));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }


    @Override public int size() {
        return delegate.size();
    }

    @Override public T get(final Object key) {
        return delegate.get(key);
    }


    @Override public T put(final Locale key, final T value) {
        throw new UnsupportedOperationException();
    }

    @Override public T remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override public void putAll(final Map<? extends Locale, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override public void clear() {
        throw new UnsupportedOperationException();
    }


    @Override public Set<Locale> keySet() {
        return delegate.keySet();
    }

    @Override public Collection<T> values() {
        return delegate.values();
    }

    @Override public Set<Entry<Locale, T>> entrySet() {
        return delegate.entrySet();
    }

}
