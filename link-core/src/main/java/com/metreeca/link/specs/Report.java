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

import java.util.AbstractList;
import java.util.Set;
import java.util.Spliterator;

/**
 * Stashed collection report.
 *
 * @param <T> the type of elements in the host collection field.
 */
public final class Report<T> extends AbstractList<T> implements Set<T> {

    public static <T> Report<T> report(final Object value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Report<>(value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Object value;


    private Report(final Object value) {
        this.value=value;
    }


    @Override public int size() {
        return 0;
    }

    @Override public T get(final int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override public Spliterator<T> spliterator() {
        return super.spliterator();
    }


    public Object value() {
        return value;
    }

}
