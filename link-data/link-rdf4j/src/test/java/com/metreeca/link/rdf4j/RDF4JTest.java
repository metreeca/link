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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.link.Frame.with;
import static com.metreeca.link.Query.Criterion.increasing;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.Stash.integer;
import static com.metreeca.link.Table.column;
import static com.metreeca.link.Table.table;
import static com.metreeca.link.rdf4j.RDF4J.rdf4j;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

        @Test void testComputeAbs() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("abs:delta", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .map(employee -> Optional
                                    .ofNullable(employee.getDelta())
                                    .map(BigDecimal::abs)
                                    .orElse(null)
                            )

                            .sorted(nullsFirst(BigDecimal::compareTo))

                            .map(v -> map(entry("value", v)))

                            .collect(toList())
                    )

            );

        }

        @Test void testComputeRound() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("round:delta", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .map(employee -> Optional
                                    .ofNullable(employee.getDelta())
                                    .map(v -> v.setScale(0, HALF_UP))
                                    .orElse(null)
                            )

                            .sorted(nullsFirst(BigDecimal::compareTo))

                            .map(v -> map(entry("value", v)))

                            .collect(toList())
                    )

            );

        }

        @Test void testComputeYear() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("year:birthdate", integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .map(employee -> integer(employee.getBirthdate().getYear()))

                            .sorted()

                            .map(v -> map(entry("value", v)))

                            .collect(toList())
                    )

            );

        }


        @Test void testReportUnknownTransform() {
            assertThatIllegalArgumentException().isThrownBy(() -> engine().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("unknown:code", ""))
                        ))
                ));

            })));
        }

    }

    @Nested
    final class Aggregating {

        @Test void testComputeCount() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("count:code", integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(List.of(map(
                            entry("value", size(Employees))
                    )))

            );

        }

        @Test void testComputeSum() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("sum:ytd", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(List.of(map(
                            entry("value", decimal(Employees.stream()
                                    .map(Employee::getYtd)
                                    .filter(Objects::nonNull)
                                    .mapToDouble(BigDecimal::doubleValue)
                                    .sum()
                            ))
                    )))

            );

        }

        @Test void testComputeAvg() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("avg:ytd", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())
                    .allSatisfy(record -> assertThat(((BigDecimal)record.get("value")).setScale(3, HALF_UP))
                            .isEqualByComparingTo(decimal(Employees.stream()
                                    .map(Employee::getYtd)
                                    .filter(Objects::nonNull)
                                    .map(v -> v.setScale(3, HALF_UP))
                                    .mapToDouble(BigDecimal::doubleValue)
                                    .average()
                                    .orElse(0)
                            ).setScale(3, HALF_UP))
                    )
            );

        }

        @Test void testComputeMin() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("min:ytd", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())
                    .allSatisfy(record -> assertThat((BigDecimal)record.get("value"))
                            .isEqualByComparingTo(decimal(Employees.stream()
                                    .map(Employee::getYtd)
                                    .filter(Objects::nonNull)
                                    .mapToDouble(BigDecimal::doubleValue)
                                    .min()
                                    .orElse(Double.NaN)
                            ))
                    )
            );

        }

        @Test void testComputeMax() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column("max:ytd", decimal(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())
                    .allSatisfy(record -> assertThat((BigDecimal)record.get("value"))
                            .isEqualByComparingTo(decimal(Employees.stream()
                                    .map(Employee::getYtd)
                                    .filter(Objects::nonNull)
                                    .mapToDouble(BigDecimal::doubleValue)
                                    .max()
                                    .orElse(Double.NaN)
                            ))
                    )

            );

        }

    }

    @Nested
    final class Grouping {

        @Test void testGroupOnRawExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("seniority"), integer(0))),
                                entry("count", column(expression("count:"), integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .collect(groupingBy(Employee::getSeniority))

                            .entrySet().stream()

                            .map(e -> map(
                                    entry("value", integer(e.getKey())),
                                    entry("count", integer(e.getValue().size()))
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testGroupOnComputedExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("year:birthdate"), integer(0))),
                                entry("count", column(expression("count:"), integer(0)))
                        ))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .collect(groupingBy(e -> e.getBirthdate().getYear()))

                            .entrySet().stream()

                            .sorted(comparingByKey())

                            .map(e -> map(
                                    entry("value", integer(e.getKey())),
                                    entry("count", integer(e.getValue().size()))
                            ))

                            .collect(toList())
                    )

            );

        }

    }

    @Nested
    final class Filtering {

        @Test void testFilterOnExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("code"), ""))
                        )),
                        filter("avg:reports.delta", lte(integer(-100_000)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .filter(employee -> Optional.ofNullable(employee.getReports())
                                    .map(reports -> reports.stream()
                                            .map(Employee::getDelta)
                                            .filter(Objects::nonNull)
                                            .mapToDouble(BigDecimal::doubleValue)
                                            .average()
                                            .orElse(0.0)
                                    )
                                    .filter(v -> v <= -100_000)
                                    .isPresent()
                            )

                            .map(Employee::getCode)
                            .sorted()

                            .map(v -> map(
                                    entry("value", v)
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testFilterOnComputedExpression() {

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("code"), ""))
                        )),
                        filter("avg:year:reports.birthdate", gte(integer(1995)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .filter(employee -> Optional.ofNullable(employee.getReports())
                                    .map(reports -> reports.stream()
                                            .map(Employee::getBirthdate)
                                            .mapToDouble(LocalDate::getYear)
                                            .average()
                                            .orElse(Double.NaN)
                                    )
                                    .filter(v -> v >= 1995)
                                    .isPresent()
                            )

                            .map(Employee::getCode)
                            .sorted()

                            .map(v -> map(
                                    entry("value", v)
                            ))

                            .collect(toList())
                    )

            );

        }


        @Test void testFilterOnProjectedExpression() {

            final Function<Employee, Double> value=employee -> Optional.ofNullable(employee.getReports())
                    .map(reports -> reports.stream()
                            .map(Employee::getDelta)
                            .filter(Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0)
                    )
                    .map(v -> (double)Math.round(v))
                    .orElse(0.0);

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("entry", column("code", "")),
                                entry("value", column("round:avg:reports.delta", decimal(0)))
                        )),
                        filter("value", lte(decimal(-100_000)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .filter(employee -> value.apply(employee) <= -100_000)

                            .map(e -> map(
                                    entry("entry", e.getCode()),
                                    entry("value", decimal(value.apply(e)).setScale(0, HALF_UP))
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testFilterOnTransformedProjectedExpression() {

            final Function<Employee, Optional<LocalDate>> value=employee -> Optional
                    .ofNullable(employee.getReports())
                    .flatMap(reports -> reports.stream()
                            .map(Employee::getBirthdate)
                            .max(LocalDate::compareTo)
                    );

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("entry", column("code", "")),
                                entry("value", column("max:reports.birthdate", LocalDate.now()))
                        )),
                        filter("year:value", gte(integer(2000)))
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .filter(employee -> value.apply(employee)
                                    .filter(v -> v.getYear() >= 2000)
                                    .isPresent()
                            )

                            .map(e -> map(
                                    entry("entry", e.getCode()),
                                    entry("value", value.apply(e).orElseThrow())
                            ))

                            .collect(toList())
                    )

            );

        }

    }

    @Nested
    final class Sorting {

        @Test void testSortOnExpression() {

            final Function<Employee, Double> value=employee -> Optional.ofNullable(employee.getReports())
                    .map(reports -> reports.stream()
                            .map(Employee::getDelta)
                            .filter(Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0)
                    )
                    .orElse(0.0);

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("code"), ""))
                        )),
                        order("avg:reports.delta", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .sorted(comparing(value).thenComparing(Employee::getCode))

                            .map(employee -> map(
                                    entry("value", employee.getCode())
                            ))

                            .collect(toList())
                    )

            );

        }

        @Test void testSortOnComputedExpression() {

            final Function<Employee, Double> value=employee -> Optional.ofNullable(employee.getReports())
                    .map(reports -> reports.stream()
                            .map(Employee::getBirthdate)
                            .mapToDouble(LocalDate::getYear)
                            .average()
                            .orElse(0.0)
                    )
                    .orElse(0.0);

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("value", column(expression("code"), ""))
                        )),
                        order("avg:year:reports.birthdate", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .sorted(comparing(value).thenComparing(Employee::getCode))

                            .map(employee -> map(
                                    entry("value", employee.getCode())
                            ))

                            .collect(toList())
                    )

            );

        }


        @Test void testSortOnProjectedExpression() {

            final Function<Employee, Double> value=employee -> Optional.ofNullable(employee.getReports())
                    .map(reports -> reports.stream()
                            .map(Employee::getDelta)
                            .filter(Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0)
                    )
                    .map(v -> (double)Math.round(v))
                    .orElse(0.0);

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("entry", column("code", "")),
                                entry("value", column("round:avg:reports.delta", decimal(0)))
                        )),
                        order("value", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .sorted(comparing(value).thenComparing(Employee::getCode))

                            .map(e -> map(
                                    entry("entry", e.getCode()),
                                    entry("value", decimal(value.apply(e)).setScale(0, HALF_UP))
                            ))

                            .collect(toList())
                    )

            );

        }


        @Test void testSortOnTransformedProjectedExpression() {

            final Function<Employee, LocalDate> value=employee -> Optional
                    .ofNullable(employee.getReports())
                    .flatMap(reports -> reports.stream()
                            .map(Employee::getBirthdate)
                            .max(LocalDate::compareTo)
                    )
                    .orElse(null);

            assertThat(testbed().retrieve(with(new Employees(), employees -> {

                employees.setId(id("/employees/"));
                employees.setMembers(query(
                        model(table(
                                entry("entry", column("code", "")),
                                entry("value", column("max:reports.birthdate", LocalDate.now()))
                        )),
                        order("year:value", increasing)
                ));

            }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                    .isEqualTo(Employees.stream()

                            .sorted(comparing(value, nullsFirst(LocalDate::compareTo))
                                    .thenComparing(Employee::getCode)
                            )

                            .map(e -> map(
                                    entry("entry", e.getCode()),
                                    entry("value", value.apply(e))
                            ))

                            .collect(toList())
                    )

            );

        }

    }

    // !!! options
    // !!! range
    // !!! stats

}