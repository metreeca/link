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
import com.metreeca.link.Table;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Frame.with;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.Stash.integer;
import static com.metreeca.link.Table.column;
import static com.metreeca.link.Table.table;
import static com.metreeca.link.rdf4j.RDF4J.rdf4j;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

final class RDF4JTest extends EngineTest {

    @Override protected Engine engine() {
        return rdf4j(new SailRepository(new MemoryStore()));
    }


    @Nested
    final class Projecting {

        @Test void testProjectEmptyTable() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table())
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .map(e -> map())

                            .collect(toList())
                    )

            );

        }

        @Test void testProjectPlainTable() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("employee", column(expression("label"), "")),
                                entry("supervisor", column(expression("supervisor.label"), ""))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .sorted(comparing((Employee employee) -> label(employee))
                                    .thenComparing(e -> label(supervisor(e)))
                            )

                            .map(e -> map(
                                    entry("employee", label(e)),
                                    entry("supervisor", label(supervisor(e)))
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testProjectTotalTable() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("employees", column(expression("count:"), integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .collect(groupingBy(e -> Employee.class))

                            .values().stream()

                            .map(employeeList -> map(
                                    entry("employees", integer(employeeList.size()))
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testProjectGroupedTable() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("seniority", column("seniority", integer(0))),
                                entry("employees", column("count:", integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .collect(groupingBy(Employee::getSeniority))
                            .entrySet().stream()

                            .map(e -> map(
                                    entry("seniority", integer(e.getKey())),
                                    entry("employees", integer(e.getValue().size()))
                            ))

                            .collect(toList())
                    )

            );

        }


        @Test void testProjectEmptyResultSet() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("employee", column(expression("label"), ""))
                        )),
                        filter("label", like("none"))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())
                    .isEmpty()
            );

        }

        @Test void testProjectModel() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("employee", column("", with(new Employee(), employee -> {
                                    employee.setLabel("");
                                    employee.setOffice(with(new Reference(), office -> office.setLabel("")));
                                }))),
                                entry("reports", column("count:reports", integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .map(record -> map(
                            entry("employee", label((Resource)record.get("employee"))),
                            entry("office", label(office((Employee)record.get("employee")))),
                            entry("reports", record.get("reports"))
                    ))

                    .isEqualTo(Employees.stream()

                            .map(employee -> map(
                                    entry("employee", label(employee)),
                                    entry("office", label(office(employee))),
                                    entry("reports", size(reports(employee)))
                            ))

                            .collect(toList())
                    ));

        }

    }

    @Nested
    final class Transforming {

        // abs

    }

    @Nested
    final class Aggregating {

        // count
        // sum
        // min
        // max
        // avg
        // sample

    }


    @Nested
    final class Grouping {

        // group on expression
        // group on computed expression
        // group on projected computed expression

    }

    @Nested
    final class Filtering {

    }

    @Nested
    final class Sorting {

        // order on expression
        // order on computed expression

    }

}