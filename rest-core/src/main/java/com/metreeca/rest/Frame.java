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

package com.metreeca.rest;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public abstract class Frame<T> {

    public static final String DefaultBase="app:/";
    public static final String DefaultSpace="app:/#";


    @SuppressWarnings("unchecked")
    public static <T> Frame<T> frame(final T object) {

        if ( object == null ) {
            throw new NullPointerException("null object");
        }

        return object instanceof Frame
                ? (Frame<T>)new FrameDelegating<>((Frame<?>)object)
                : new FrameReflective<>(object);
    }

    public static <T> Frame<T> frame(final Class<T> clazz) {

        if ( clazz == null ) {
            throw new NullPointerException("null class");
        }

        return new FrameReflective<>(clazz);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> T with(final T value, final Consumer<T> consumer) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( consumer == null ) {
            throw new NullPointerException("null consumer");
        }

        consumer.accept(value);

        return value;
    }


    public static Optional<String> absolute(final String iri) {
        return Lingo.absolute(iri).map(Matcher::group);
    }

    public static Optional<String> base(final String iri) {
        return Lingo.base(iri);
    }

    public static Optional<String> path(final String iri) {
        return Lingo.path(iri);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Frame() { }


    public abstract Frame<T> copy();


    public abstract T value();

    public abstract Shape shape();


    public abstract String id();

    public abstract Frame<T> id(final String id);


    public abstract Stream<Entry<String, Object>> entries(final boolean id);

    public abstract Object get(final String field);

    public abstract Frame<T> set(final String field, final Object value);


    public Optional<Trace> validate() {
        return Optional.empty(); // !!!
    }

}
