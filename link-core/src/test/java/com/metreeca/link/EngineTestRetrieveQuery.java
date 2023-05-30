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
import com.metreeca.link.EngineTest.Employees;
import com.metreeca.link.EngineTest.Reference;
import com.metreeca.link.EngineTest.Resource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.link.EngineTest.Employees;
import static com.metreeca.link.EngineTest.id;
import static com.metreeca.link.Frame.with;
import static com.metreeca.link.Query.Criterion.decreasing;
import static com.metreeca.link.Query.Criterion.increasing;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.Stash.integer;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class EngineTestRetrieveQuery {

    protected abstract Engine testbed();

    @Nested
    final class Fetching {

        @Test void testFetchEmptyResultSet() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("label", like("none"))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactly()
            );

        }

        @Test void testFetchEmptyModel() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(new Employee())
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(Resource::getId)
                    .containsExactlyElementsOf(Employees.stream()
                            .map(e -> (String)null)
                            .collect(toList())
                    ));

        }

        @Test void testFetchModel() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> {
                            employee.setId("");
                            employee.setLabel("");
                            employee.setSeniority(0);
                        }))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .allSatisfy(employee -> {

                        final Employee expected=Employees.stream()
                                .filter(e -> id(e.getId()).equals(id(employee.getId())))
                                .findFirst()
                                .orElseThrow();

                        // specified by model

                        assertThat(employee.getLabel()).isEqualTo(expected.getLabel());
                        assertThat(employee.getSeniority()).isEqualTo(expected.getSeniority());

                        // not specified by model

                        assertThat(employee.getSurname()).isNull();
                        assertThat(employee.getSupervisor()).isNull();
                        assertThat(employee.getReports()).isNull();

                    })
            );

        }

        @Test void testFetchNestedModel() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> {
                            employee.setId("");
                            employee.setSupervisor(with(new Employee(), supervisor -> {
                                supervisor.setLabel("");
                            }));
                        }))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .allSatisfy(employee -> {

                        final Employee expected=Employees.stream()
                                .filter(e -> id(e.getId()).equals(id(employee.getId())))
                                .findFirst()
                                .orElseThrow();

                        assertThat(Optional.ofNullable(employee.getSupervisor()).map(Resource::getLabel))
                                .isEqualTo(Optional.ofNullable(expected.getSupervisor()).map(Resource::getLabel));

                    })
            );

        }

    }

    @Nested
    final class Filtering {

        @Test void testHandleLtConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("seniority", lt(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getSeniority() < 3)
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleGtConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("seniority", gt(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getSeniority() > 3)
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleLteConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("seniority", lte(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getSeniority() <= 3)
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleGteConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("seniority", gte(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getSeniority() >= 3)
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }


        @Test void testHandleLikeConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("label", like("ger bo"))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getLabel().equals("Gerard Bondur"))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleMultipleLikeConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees ->
            {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("label", like("ger")),
                        filter("label", like("bo"))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> employee.getLabel().equals("Gerard Bondur"))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }


        @Test void testHandleRootAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("", any(
                                with(new Reference(), reference -> reference.setId("/employees/1056")),
                                with(new Reference(), reference -> reference.setId("/employees/1088"))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee
                                    -> employee.getId().equals(id("/employees/1056"))
                                    || employee.getId().equals(id("/employees/1088"))
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleSingletonAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor",
                                any(with(new Reference(), reference -> reference.setId("/employees/1088")))
                        )
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .filter(supervisor -> supervisor.getId().equals(id("/employees/1088")))
                                    .isPresent()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleMultipleAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor", any(
                                with(new Reference(), reference -> reference.setId("/employees/1056")),
                                with(new Reference(), reference -> reference.setId("/employees/1088"))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .filter(supervisor
                                            -> supervisor.getId().equals(id("/employees/1056"))
                                            || supervisor.getId().equals(id("/employees/1088"))
                                    )
                                    .isPresent()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleExistentialAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor", any())
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .isPresent()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleNonExistentialAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor", any((Object)null))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .isEmpty()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleMixedAnyConstraints() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor", any(null, with(new Reference(), s -> s.setId(id("/employees/1002")))))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee
                                    -> employee.getSupervisor() == null
                                    || id(employee.getSupervisor().getId()).equals(id("/employees/1002"))
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }


        @Test void testFilterOnExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("supervisor.seniority", gte(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .filter(supervisor -> supervisor.getSeniority() >= 3)
                                    .isPresent()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Disabled
        @Test void testFilterOnComputedExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        filter("abs:seniority", gte(integer(3)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .filter(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .filter(supervisor -> Math.abs(supervisor.getSeniority()) >= 3)
                                    .isPresent()
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

    }

    @Nested
    final class Sorting {

        @Test void testSortByDefault() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId("")))
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getId))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testSortOnRoot() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        order("", decreasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getId).reversed())
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testSortOnFieldIncreasing() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        order("label", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getLabel))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testSortOnFieldDecreasing() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        order("label", decreasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getLabel).reversed())
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testSortOnExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        order("supervisor.label", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(employee -> Optional.ofNullable(employee.getSupervisor())
                                    .map(Resource::getLabel)
                                    .orElse("")
                            ))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testSortOnMultipleCriteria() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        order("seniority", decreasing),
                        order("label", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Employee::getSeniority).reversed()
                                    .thenComparing(comparing(Employee::getLabel))
                            )
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }


        @Test void testHandleRange() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        offset(5),
                        limit(10)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getId))
                            .skip(5)
                            .limit(10)
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

        @Test void testHandleDefaultRange() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(with(new Employee(), employee -> employee.setId(""))),
                        offset(0),
                        limit(0)
                ));

            }))).hasValueSatisfying(employees -> assertThat(employees.getMembers())
                    .map(employee -> id(employee.getId()))
                    .containsExactlyElementsOf(Employees.stream()
                            .sorted(comparing(Resource::getId))
                            .map(Resource::getId)
                            .collect(toList())
                    )
            );

        }

    }

}
