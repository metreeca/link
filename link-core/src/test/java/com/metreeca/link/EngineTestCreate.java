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

import com.metreeca.link.EngineTest.Employee;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.link.EngineTest.id;
import static com.metreeca.link.Frame.with;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public abstract class EngineTestCreate {

    protected abstract Engine engine();


    @Test void testCreateResource() {

        final Engine engine=engine();

        final Employee expected=with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setLabel("Tino Faussone");
            e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/2"))));

        });

        assertThat(engine.create(expected))
                .contains(expected);

        assertThat(engine.retrieve(with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setLabel("");
            e.setSupervisor(with(new Employee(), s -> s.setId("")));

        })))

                .hasValueSatisfying(actual -> {

                    assertThat(id(actual.getId())).isEqualTo(expected.getId());
                    assertThat(actual.getLabel()).isEqualTo(expected.getLabel());
                    assertThat(id(actual.getSupervisor().getId())).isEqualTo(expected.getSupervisor().getId());

                });
    }

    @Test void testIgnorePropertiesOfNestedObjects() {

        final Engine engine=engine();

        final Employee expected=with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setSupervisor(with(new Employee(), s -> {
                s.setId(id("/employees/2"));
                s.setLabel("Tino Faussone");
            }));

        });

        engine.create(expected);

        assertThat(engine.retrieve(with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setSupervisor(with(new Employee(), s -> {
                s.setId("");
                s.setLabel("");
            }));

        })))

                .hasValueSatisfying(actual -> {

                    assertThat(id(actual.getId())).isEqualTo(expected.getId());
                    assertThat(actual.getSupervisor()).satisfies(supervisor -> {
                        assertThat(id(supervisor.getId())).isEqualTo(expected.getSupervisor().getId());
                        assertThat(supervisor.getLabel()).isNull();
                    });

                });
    }

    @Test void testHandleCollections() {

        final Engine engine=engine();

        final Employee expected=with(new Employee(), e -> {

            e.setId(id("/employees/1"));

            e.setReports(Set.of(
                    with(new Employee(), s -> s.setId(id("/employees/2"))),
                    with(new Employee(), s -> s.setId(id("/employees/3"))),
                    with(new Employee(), s -> s.setId(id("/employees/4")))
            ));

        });

        expected.getReports().forEach(engine::create);

        engine.create(expected);

        assertThat(engine.retrieve(with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setReports(Set.of(with(new Employee(), r -> r.setId(""))));

        })))

                .hasValueSatisfying(actual -> {

                    assertThat(id(actual.getId())).isEqualTo(expected.getId());
                    assertThat(actual.getReports())
                            .map(EngineTest.Resource::getId)
                            .map(EngineTest::id)
                            .containsExactlyInAnyOrder(expected.getReports().stream()
                                    .map(EngineTest.Resource::getId)
                                    .toArray(String[]::new)
                            );
                });

    }


    @Test void testReportConflictingResources() {

        final Engine engine=engine();

        final Employee expected=with(new Employee(), e -> {

            e.setId(id("/employees/1"));
            e.setSupervisor(with(new Employee(), s -> {
                s.setId(id("/employees/2"));
                s.setLabel("Tino Faussone");
            }));

        });

        engine.create(expected);

        assertThat(engine.create(expected))
                .isEmpty();

    }

    @Test void testReportMissingId() {

        final Engine engine=engine();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.create(new Employee()));
    }

}
