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

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.metreeca.rest.Glass.glass;

final class FrameReflective<T> extends Frame<T> {

    private final T object;
    private final Shape shape;

    private final Glass<T> glass;


    FrameReflective(final Class<T> clazz) {
        this.glass=glass(clazz);
        this.object=glass.create();
        this.shape=Shape.shape(clazz);
    }

    FrameReflective(final T object) {
        this.glass=glass(object.getClass());
        this.object=object;
        this.shape=Shape.shape(object.getClass());
    }

    @Override public Frame<T> copy() {
        return new FrameReflective<>(glass.create(object));
    }


    @Override public T value() {
        return object;
    }

    @Override public Shape shape() {
        return shape;
    }


    @Override public String id() {
        return shape.id().map(field -> (String)glass.get(object, field)).orElse(null);
    }

    @Override public Frame<T> id(final String id) {

        shape.id().ifPresent(field -> glass.set(object, field, id));

        return this;
    }


    @Override public Stream<Entry<String, Object>> entries(final boolean id) {
        return glass.properties().entrySet().stream()
                .filter(entry -> id || shape.id().filter(i -> i.equals(entry.getKey())).isEmpty())
                .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().get(object))); // ;( handle nulls
    }

    @Override public Object get(final String field) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        return glass.get(object, field);
    }

    @Override public Frame<T> set(final String field, final Object value) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        glass.set(object, field, value);

        return this;
    }

}
