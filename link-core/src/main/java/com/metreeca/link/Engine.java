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

import java.util.Optional;

/**
 * Data storage engine.
 *
 * <p>Handles CRUD operations on resources managed by a data storage backend.</p>
 */
public interface Engine {

    /**
     * Handles retrieval requests.
     *
     * @param model the model for resource to be retrieved, possibly containing collection filters
     * @param <V>   the type of the resource to be retrieved
     * @return an optional containing a description of the retrieved resource modelled after {@code model}, if its
     * {@linkplain Frame#id() id} was present in the storage backend; an empty optional, otherwise
     * @throws NullPointerException     if {@code model} is null
     * @throws IllegalArgumentException if {@code model} doesn't define a well-formed id, according to driver-specific
     *                                  rules
     */
    public <V> Optional<V> retrieve(final V model);


    /**
     * Handles creation requests.
     *
     * <p><strong>Warning</strong> / {@code value} is expected to be already validated.</p>
     *
     * @param value the resource to be created
     * @param <V>   the type of the resource to be created
     *
     * @return an optional containing {@code value}, if its {@linkplain Frame#id() id} was not already present in the
     * storage backend; an empty optional, otherwise
     *
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} doesn't define a well-formed id, according to driver-specific
     *                                  rules
     */
    public <V> Optional<V> create(final V value);

    /**
     * Handles updating requests.
     *
     * <p><strong>Warning</strong> / {@code value} is expected to be already validated.</p>
     *
     * @param value the resource to be updated
     * @param <V>   the type of the resource to be updated
     *
     * @return an optional containing {@code value}, if its {@linkplain Frame#id() id} was present in the storage
     * backend; an empty optional, otherwise
     *
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} doesn't define a well-formed id, according to driver-specific
     *                                  rules
     */
    public <V> Optional<V> update(final V value);

    /**
     * Handles deletion requests.
     *
     * @param value the resource to be deleted
     * @param <V>   the type of the resource to be deleted
     *
     * @return an optional containing {@code value}, if its {@linkplain Frame#id() id} was present in the storage
     * backend; an empty optional, otherwise
     *
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} doesn't define a well-formed id, according to driver-specific
     *                                  rules
     */
    public <V> Optional<V> delete(final V value);

}
