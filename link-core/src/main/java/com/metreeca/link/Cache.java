/*
 * Copyright © 2023-2024 Metreeca srl
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

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class Cache<T> implements Supplier<T> {

    private final Supplier<? extends T> factory;

    private T value;


    Cache(final Supplier<? extends T> factory) {
        this.factory=factory;
    }


    @Override public T get() {
        return (value != null) ? value : (value=requireNonNull(
                factory.get(), "null factory return value"
        ));
    }

}
