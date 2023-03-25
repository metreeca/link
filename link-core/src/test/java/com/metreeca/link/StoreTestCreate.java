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
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.link.Frame.*;
import static com.metreeca.link.StoreTest.*;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class StoreTestCreate {

    // !!! cascade composite shapes

    protected abstract Store store();


    @Test void testIgnoreConflictingResources() {

        final Store store=store();

        final IRI id=item("/employees/1");

        final Frame expected=frame(
                field(RDFS.LABEL, literal(""))
        );

        store.create(id, Employee(), expected);

        assertThat(store.create(id, Employee(), expected))
                .isFalse();

    }

    @Test void testCreateResource() {

        final Store store=store();

        final IRI id=item("/employees/1");

        final Frame expected=frame(

                field(RDFS.LABEL, literal("Tino Faussone")),
                field(SUPERVISOR, frame(
                        field(ID, item("/employees/2"))
                ))

        );

        assertThat(store.create(id, Employee(), expected))
                .isTrue();

        assertThat(store.retrieve(id, Employee(), frame(

                field(RDFS.LABEL, literal("")),
                field(SUPERVISOR, frame(
                        field(ID, iri())
                ))

        )))

                .hasValueSatisfying(actual -> {

                    assertThat(actual.id())
                            .isEqualTo(expected.id());

                    assertThat(actual.value(RDFS.LABEL))
                            .isEqualTo(expected.value(RDFS.LABEL));

                    assertThat(actual.value(SUPERVISOR, asFrame()).map(Frame::id))
                            .isEqualTo(expected.value(SUPERVISOR, asFrame()).map(Frame::id));

                });
    }

    @Test void testIgnorePropertiesOfNestedObjects() {

        final Store store=store();

        final IRI id=item("/employees/1");

        final Frame expected=frame(
                field(SUPERVISOR, frame(
                        field(ID, item("/employees/2")),
                        field(RDFS.LABEL, literal("Tino Faussone"))
                ))
        );

        store.create(id, Employee(), expected);

        assertThat(store.retrieve(id, Employee(), frame(
                field(SUPERVISOR, frame(
                        field(ID, iri()),
                        field(RDFS.LABEL, literal(""))
                ))
        )))

                .hasValueSatisfying(actual -> {

                    assertThat(actual.id()).isEqualTo(expected.id());
                    assertThat(actual.value(SUPERVISOR, asFrame())).hasValueSatisfying(supervisor -> {
                        assertThat(supervisor.id()).isEqualTo(expected.value(SUPERVISOR, asFrame()).flatMap(Frame::id));
                        assertThat(supervisor.value(RDFS.LABEL)).isEmpty();
                    });

                });
    }

    @Test void testHandleCollections() {

        final Store store=store();

        final IRI id=item("/employees/1");

        final Frame expected=frame(
                field(REPORT,
                        frame(
                                field(ID, item("/employees/2")),
                                field(RDFS.LABEL, literal("2"))
                        ),
                        frame(
                                field(ID, item("/employees/3")),
                                field(RDFS.LABEL, literal("3"))
                        ),
                        frame(
                                field(ID, item("/employees/4")),
                                field(RDFS.LABEL, literal("4"))
                        )
                )
        );

        expected.values(REPORT, asFrame()).forEach(frame -> frame.id().ifPresent(iri ->
                store.create(iri, Employee(), frame)
        ));

        store.create(id, Employee(), expected);

        assertThat(store.retrieve(id, Employee(), frame(
                field(REPORT, frame(field(ID, iri())))
        )))

                .hasValueSatisfying(actual -> {

                    assertThat(actual.id()).isEqualTo(expected.id());

                    assertThat(actual.values(REPORT, asFrame())
                            .map(Frame::id)
                            .flatMap(Optional::stream)
                            .collect(toList())
                    )
                            .containsExactlyInAnyOrder(actual.values(REPORT, asFrame())
                                    .map(Frame::id)
                                    .flatMap(Optional::stream)
                                    .toArray(IRI[]::new)
                            );
                });

    }

    @Test void testHandleLoops() {

        final Store store=store();

        final IRI id=item("/employees/1");

        final Frame employee=frame(
                field(SUPERVISOR, frame(
                        field(ID, id)
                ))
        );

        assertThat(store.create(id, Employee(), employee))
                .isTrue();

        assertThat(store.retrieve(id, Employee(), frame(
                field(ID, id),
                field(SUPERVISOR, frame(
                        field(ID, iri())
                ))
        )))
                .hasValueSatisfying(actual -> assertThat(actual.value(SUPERVISOR, asFrame()).flatMap(Frame::id))
                        .isEqualTo(actual.id())
                );
    }

}
