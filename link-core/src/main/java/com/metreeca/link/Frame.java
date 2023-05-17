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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static com.metreeca.link.Query.filter;
import static com.metreeca.link.Query.query;

import static java.util.stream.Collectors.toList;

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

    public static Optional<String> root(final String iri) {
        return Lingo.root(iri);
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

    public Optional<Frame<T>> merge(final Frame<T> specs) {

        if ( specs == null ) {
            throw new NullPointerException("null specs");
        }

        final Frame<T> copy=copy();

        if ( copy.value().getClass().isAssignableFrom(specs.value().getClass()) ) {

            copy.entries(true).forEach(entry -> {

                final String field=entry.getKey();

                final Object value=entry.getValue();
                final Object model=specs.get(field);

                if ( model instanceof Query ) { // merge filters

                    final Query<Object> filters=query(((Query<?>)model)
                            .filters().entrySet().stream()
                            .map(filter -> filter(filter.getKey(), filter.getValue()))
                            .collect(toList())
                    );

                    if ( value instanceof Query ) {

                        copy.set(field, query((Query<?>)value, filters));

                    } else if ( value instanceof Collection ) {

                        // !!! merge specs filters
                        // !!! handles 0/1/multiple items

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    } else if ( value != null ) {

                        // !!! merge specs filters
                        // !!! ignore? report?

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    }

                } else if ( model != null && !(value instanceof Query) ) { // merge specs to support virtual entities

                    if (

                            value instanceof Boolean && value.equals(false)
                                    || value instanceof Number && ((Number)value).intValue() == 0
                                    || value instanceof String && ((String)value).isBlank()
                                    || value instanceof Collection && ((Collection<?>)value).isEmpty()

                    ) {

                        copy.set(field, model);

                    }

                }

            });

            return Optional.of(copy);

        } else {

            return Optional.empty();

        }
    }

}
