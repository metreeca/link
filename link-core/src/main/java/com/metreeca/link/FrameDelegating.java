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

import java.util.Map.Entry;
import java.util.stream.Stream;

final class FrameDelegating<T> extends Frame<Frame<T>> {

    private final Frame<T> frame;


    FrameDelegating(final Frame<T> frame) { this.frame=frame; }


    @Override public Frame<Frame<T>> copy() {
        return new FrameDelegating<>(frame.copy());
    }


    @Override public Frame<T> value() { return frame; }

    @Override public Shape shape() { return frame.shape(); }


    @Override public String id() { return frame.id(); }

    @Override public Frame<Frame<T>> id(final String id) {

        frame.id(id);

        return this;
    }


    @Override public Stream<Entry<String, Object>> entries(final boolean id) { return frame.entries(id); }

    @Override public Object get(final String field) { return frame.get(field); }

    @Override public Frame<Frame<T>> set(final String field, final Object value) {

        frame.set(field, value);

        return this;
    }

}
