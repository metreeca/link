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

import org.eclipse.rdf4j.model.IRI;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Data storage engine.
 *
 * <p>Handles CRUD operations on resources managed by a data storage backend.</p>
 */
public interface Store {

    /**
     * Handles retrieval requests.
     *
     * <p><strong>Warning</strong> / Storage engines are not required to perform {@linkplain Shape#validate(Frame)
     * validation}: {@code model} is expected to be consistent with the provided {@code shape}; detected inconsistencies
     * may cause the operation to silently fail.</p>
     *
     * @param id    the identifier of the resource to be retrieved
     * @param shape the shape of the resource to be retrieved
     * @param model the model for resource to be retrieved, possibly containing collection {@linkplain Query queries}
     * @param langs preferred languages for retrieval and sorting operations, in order of priority; may include a
     *              wildcard tag ({@code *})
     *
     * @return an optional containing a description of the retrieved resource modelled after {@code model}, if
     * {@code id} was present in the storage backend; an empty optional, otherwise
     *
     * @throws NullPointerException if any parameter is null or contains null values
     */
    public default Optional<Frame> retrieve(final IRI id, final Shape shape, final Frame model, final String... langs) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( langs == null || Arrays.stream(langs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        return retrieve(id, shape, model, List.of(langs));
    }

    /**
     * Handles retrieval requests.
     *
     * <p><strong>Warning</strong> / Storage engines are not required to perform {@linkplain Shape#validate(Frame)
     * validation}: {@code model} is expected to be consistent with the provided {@code shape}; detected inconsistencies
     * may cause the operation to silently fail.</p>
     *
     * @param id    the identifier of the resource to be retrieved
     * @param shape the shape of the resource to be retrieved
     * @param model the model for resource to be retrieved, possibly containing collection {@linkplain Query queries}
     * @param langs preferred languages for retrieval and sorting operations, in order of priority; may include a
     *              wildcard tag ({@code *})
     *
     * @return an optional containing a description of the retrieved resource modelled after {@code model}, if
     * {@code id} was present in the storage backend; an empty optional, otherwise
     *
     * @throws NullPointerException if any parameter is null or contains null values
     */
    public Optional<Frame> retrieve(final IRI id, final Shape shape, final Frame model, final List<String> langs);


    /**
     * Handles creation requests.
     *
     * <p><strong>Warning</strong> / Storage engines are not required to perform {@linkplain Shape#validate(Frame)
     * validation}: {@code state} is expected to be consistent with the provided {@code shape}; detected inconsistencies
     * may cause the operation to silently fail.</p>
     *
     * @param id    the identifier of the resource to be created
     * @param shape the shape of the resource to be created
     * @param state the description of resource to be created
     *
     * @return {@code true}, if {@code id} was not present in the storage backend and a new resource was actually
     * created; {@code false}, otherwise
     *
     * @throws NullPointerException if any parameter is null
     */
    public boolean create(final IRI id, final Shape shape, final Frame state);

    /**
     * Handles updating requests.
     *
     * <p><strong>Warning</strong> / Storage engines are not required to perform {@linkplain Shape#validate(Frame)
     * validation}: {@code frame} is expected to be consistent with the provided {@code shape}; detected inconsistencies
     * may cause the operation to silently fail.</p>
     *
     * @param id    the identifier of the resource to be updated
     * @param shape the shape of the resource to be updated
     * @param state the description of resource to be updated
     *
     * @return {@code true}, if {@code id} was present in the storage backend and the resource was actually updated;
     * {@code false}, otherwise
     *
     * @throws NullPointerException if any parameter is null
     */
    public boolean update(final IRI id, final Shape shape, final Frame state);

    /**
     * Handles deletion requests.
     *
     * @param id    the identifier of the resource to be deleted
     * @param shape the shape of the resource to be deleted
     *
     * @return {@code true}, if {@code id} was present in the storage backend and the resource was actually deleted;
     * {@code false}, otherwise
     *
     * @throws NullPointerException if either {@code shape} or {@code frame} is null
     */
    public boolean delete(final IRI id, final Shape shape);

}
