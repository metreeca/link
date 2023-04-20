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

package com.metreeca.link.rdf4j;

import com.metreeca.link.Engine;
import com.metreeca.link.EngineTest;
import com.metreeca.link.toys.*;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.metreeca.link.Frame.with;
import static com.metreeca.link.rdf4j.RDF4J.rdf4j;
import static com.metreeca.link.toys.Employees.Employees;
import static com.metreeca.link.toys.Resource.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class RDF4JTest extends EngineTest {

    @Override protected Engine engine() {

        final RDF4J engine=rdf4j(new SailRepository(new MemoryStore()));

        Employees.forEach(engine::create);

        return engine;
    }


    @Nested final class Retrieve {

        @Test void testX() {

            final Engine engine=engine();

            final String relative="/employees/1002";
            final String absolute=id(relative);

            final Employee expected=Employees.stream()
                    .filter(e -> e.getId().equals(absolute))
                    .findFirst()
                    .orElseThrow();

            final Optional<Employee> actual=engine.retrieve(with(new Employee(), e -> {

                e.setId(absolute);
                e.setLabel("");

            }));

            assertThat(actual).hasValueSatisfying(e -> assertThat(e.getId()).isEqualTo(relative));

        }

    }


    @Nested final class Create {

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
                                .map(Resource::getId)
                                .map(Resource::id)
                                .containsExactlyInAnyOrder(expected.getReports().stream()
                                        .map(Resource::getId)
                                        .toArray(String[]::new)
                                );
                    });

        }

        @Test void testIgnoreExistingResources() {

            final Engine engine=engine();

            Offices.Offices.forEach(engine::create);

            final Office create=with(new Office(), office -> {

                office.setId(id("/offices/1"));
                office.setLabel("Berkeley (USA)");

            });

            assertThat(engine.create(create))
                    .isEmpty();

        }

        @Test void testReportMissingId() {

            final Engine engine=engine();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> engine.create(new Employee()));
        }

    }

    @Nested final class Update {

        @Test void testUpdate() {

            final Engine engine=engine();

            Offices.Offices.forEach(engine::create);

            final Office update=with(new Office(), o -> {

                o.setId(id("/offices/1"));
                o.setLabel("Berkeley (USA)");

            });

            assertThat(engine.update(update))
                    .hasValueSatisfying(office -> assertThat(office.getId())
                            .isEqualTo(update.getId())
                    );


            assertThat(engine.retrieve(with(new Office(), o -> {

                o.setId(update.getId());
                o.setLabel("");

            })))

                    .hasValueSatisfying(actual -> {

                        assertThat(id(actual.getId())).isEqualTo(update.getId());
                        assertThat(actual.getLabel()).isEqualTo(update.getLabel());

                    });


        }

        @Test void testIgnoreMissingResources() {

            final Engine engine=engine();

            final Office update=with(new Office(), office -> {

                office.setId(id("/employees/999"));
                office.setLabel("Memmo Cancelli");

            });

            assertThat(engine.update(update))
                    .isEmpty();

        }

        @Test void testReportMissingId() {

            final Engine engine=engine();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> engine.update(new Employee()));
        }

    }

    @Nested final class Delete {

        @Test void testDelete() {

            final Engine engine=engine();

            Offices.Offices.forEach(engine::create);

            final Office delete=with(new Office(), o -> o.setId(id("/offices/1")));

            assertThat(engine.delete(delete))
                    .hasValueSatisfying(office -> assertThat(office.getId())
                            .isEqualTo(delete.getId())
                    );

            assertThat(engine.retrieve(with(new Office(), o -> o.setId(delete.getId()))))
                    .isEmpty();


        }

        @Test void testIgnoreMissingResources() {

            final Engine engine=engine();

            final Office delete=with(new Office(), office -> office.setId(id("/employees/999")));

            assertThat(engine.delete(delete))
                    .isEmpty();

        }

        @Test void testReportMissingId() {

            final Engine engine=engine();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> engine.delete(new Employee()));
        }

    }

}