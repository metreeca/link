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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;

final class _FrameDeclarative extends Frame<Map<String, Object>> {

    public static Frame<Map<String, Object>> frame(final String id, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return new _FrameDeclarative(id, shape);
    }

    private final String id;
    private final Shape shape;

    private final Map<String, Object> entries=new LinkedHashMap<>();


    _FrameDeclarative(final String id, final Shape shape) {
        this.id=id;
        this.shape=shape;
    }


    @Override public Frame<Map<String, Object>> copy() {

        final _FrameDeclarative copy=new _FrameDeclarative(id, shape);

        copy.entries.putAll(entries);

        return copy;
    }


    @Override public Map<String, Object> value() { return unmodifiableMap(entries); }

    @Override public Shape shape() { return shape; }


    @Override public String id() { return (String)entries.get(id); }

    @Override public Frame<Map<String, Object>> id(final String id) {

        entries.put(this.id, id);

        return this;
    }


    @Override public Stream<Entry<String, Object>> entries(final boolean id) {
        return entries.entrySet().stream()
                .filter(entry -> id || !entry.getKey().equals(this.id));
    }

    @Override public Object get(final String field) { return entries.get(field); }

    @Override public Frame<Map<String, Object>> set(final String field, final Object value) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        if ( field.equals(id) && !(value == null || value instanceof String) ) {
            throw new IllegalArgumentException(format(
                    "value <%s> for id field <%s> is not a string", id, value
            ));
        }

        entries.put(field, value);

        return this;
    }

}
