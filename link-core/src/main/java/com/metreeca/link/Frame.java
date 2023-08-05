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

import com.metreeca.link.specs.Query;
import com.metreeca.link.specs.Specs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.metreeca.link.specs.Query.query;
import static com.metreeca.link.specs.Specs.filter;
import static com.metreeca.link.specs.Specs.specs;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public abstract class Frame<T> {

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


    //// Factory Helpers ///////////////////////////////////////////////////////////////////////////////////////////////

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


    public static BigInteger integer(final long value) {
        return BigInteger.valueOf(value);
    }

    public static BigDecimal decimal(final double value) {
        return BigDecimal.valueOf(value);
    }


    public static <T> T error(final String format, final Object... args) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        if ( args == null ) {
            throw new NullPointerException("null args");
        }

        throw new IllegalArgumentException(format(format, args));
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


    public Optional<Frame<T>> merge(final Frame<T> frame) {

        if ( frame == null ) {
            throw new NullPointerException("null frame");
        }

        final Frame<T> copy=copy();

        if ( copy.value().getClass().isAssignableFrom(frame.value().getClass()) ) {

            copy.entries(true).forEach(entry -> {

                final String field=entry.getKey();

                final Object value=entry.getValue();
                final Object model=frame.get(field);

                if ( model instanceof Query ) { // merge filters

                    final Specs filters=specs(((Query<?>)model).specs()
                            .filter().entrySet().stream()
                            .map(filter -> filter(filter.getKey(), filter.getValue()))
                            .collect(toList())
                    );

                    if ( value instanceof Query ) {

                        copy.set(field, query(((Query<?>)value).model(), specs(((Query<?>)value).specs(), filters)));

                    } else if ( value instanceof Collection ) {

                        // !!! merge frame filters
                        // !!! handles 0/1/multiple items

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    } else if ( value != null ) {

                        // !!! merge frame filters
                        // !!! ignore? report?

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    }

                } else if ( model != null ) { // merge default values from frame to support virtual entities

                    if (

                            isBooleanDefault(value)
                                    || isNumberDefault(value)
                                    || isStringDefault(value)
                                    || isCollectionDefault(value)

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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Trace> validate() {
        return Optional.empty(); // !!!
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isBooleanDefault(final Object value) {
        return value instanceof Boolean && value.equals(false);
    }

    private static boolean isNumberDefault(final Object value) {
        return value instanceof Number && ((Number)value).intValue() == 0;
    }

    private static boolean isStringDefault(final Object value) {
        return value instanceof String && ((String)value).isBlank();
    }

    private static boolean isCollectionDefault(final Object value) {
        return value instanceof Collection && !(value instanceof Query) && ((Collection<?>)value).isEmpty();
    }

}
