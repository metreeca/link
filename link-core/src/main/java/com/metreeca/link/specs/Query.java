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

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.error;

/**
 * Stashed collection query.
 *
 * @param <T> the type of elements in the host collection field.
 */
public final class Query<T> extends AbstractList<T> implements Set<T> {

    public static <T> Query<T> query(final Object model, final Specs... specs) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( specs == null || Arrays.stream(specs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null specs");
        }

        final Specs spec=Specs.specs(specs);

        if ( model instanceof Table ) {

            Stream.of(spec.filter(), spec.focus(), spec.order())

                    .map(Map::keySet)
                    .flatMap(Collection::stream)
                    .map(Expression::path)

                    .filter(path -> path.size() > 1)
                    .filter(path -> ((Table)model).containsKey(path.get(0)))

                    .forEach(path -> error("expression path <%s> refers to a field of a projected value", path));

        }

        return new Query<>(model, spec);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Object model;
    private final Specs specs;


    private Query(final Object model, final Specs specs) {
        this.model=model;
        this.specs=specs;
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


    public Object model() {
        return model;
    }

    public Specs specs() {
        return specs;
    }

}
