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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.StoreTest.*;
import static com.metreeca.link.Transform.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class StoreTestRetrieveIndex {

    protected abstract Store store();


    @Nested
    final class Fetching {

        @Test void testFetchEmptyModel() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()).collect(toList()))
                    .hasSameSizeAs(EMPLOYEES)
            );
        }

        @Test void testFetchEmptyResultSet() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(),

                            filter(RDF.TYPE, EMPLOYEE_TYPE),
                            filter(RDFS.LABEL, like("none"))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .isEmpty()
            );
        }

        @Test void testFetchModel() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(ID, iri()),
                                    field(RDFS.LABEL, literal("")),
                                    field(SENIORITY, literal(0))
                            ),

                            filter(RDF.TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))

                    .allSatisfy(employee -> {

                        final Frame expected=EMPLOYEES.stream()
                                .filter(e -> e.id().equals(employee.id()))
                                .findFirst()
                                .orElseThrow();

                        // specified by model

                        assertThat(employee.value(RDFS.LABEL)).isEqualTo(expected.value(RDFS.LABEL));
                        assertThat(employee.value(SENIORITY)).isEqualTo(expected.value(SENIORITY));

                        // not specified by model

                        assertThat(employee.value(SURNAME)).isEmpty();
                        assertThat(employee.value(SUPERVISOR)).isEmpty();
                        assertThat(employee.value(BIRTHDATE)).isEmpty();

                    })

            );
        }

        @Test void testFetchNestedModel() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(ID, iri()),
                                    field(SUPERVISOR, frame(
                                            field(RDFS.LABEL, literal(""))
                                    ))
                            ),

                            filter(RDF.TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))

                    .allSatisfy(employee -> assertThat(employee.value(SUPERVISOR, asFrame())

                            .flatMap(supervisor -> supervisor.value(RDFS.LABEL))

                    ).isEqualTo(Employee(employee)
                            .flatMap(frame -> frame.value(SUPERVISOR, asFrame()))
                            .flatMap(StoreTest::Employee)
                            .flatMap(supervisor -> supervisor.value(RDFS.LABEL))
                    ))

            );
        }

    }

    @Nested
    final class Filtering {

        @Test void testHandleLtConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SENIORITY, lt(literal(integer(3))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SENIORITY, asInt()).orElse(0) < 3)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleGtConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SENIORITY, gt(literal(integer(3))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SENIORITY, asInt()).orElse(0) > 3)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleLteConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SENIORITY, lte(literal(integer(3))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SENIORITY, asInt()).orElse(0) <= 3)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleGteConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SENIORITY, gte(literal(integer(3))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SENIORITY, asInt()).orElseThrow() >= 3)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testHandleLikeConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(RDFS.LABEL, like("ger bo"))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(RDFS.LABEL, asString()).orElseThrow().equals("Gerard Bondur"))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testHandleRootAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(expression(List.of(), List.of()), any(
                                    frame(field(ID, item("/employees/1056"))),
                                    frame(field(ID, item("/employees/1088")))
                            ))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .map(Frame::id)
                            .filter(id -> Set.of(
                                    item("/employees/1056"),
                                    item("/employees/1088")
                            ).contains(id.orElseThrow()))
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleSingletonAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SUPERVISOR, any(
                                    item("/employees/1088")
                            ))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SUPERVISOR, asFrame())
                                    .flatMap(Frame::id)
                                    .filter(id -> id.equals(item("/employees/1088")))
                                    .isPresent()
                            )
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleMultipleAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SUPERVISOR, any(
                                    item("/employees/1056"),
                                    item("/employees/1088")
                            ))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SUPERVISOR, asFrame())
                                    .flatMap(Frame::id)
                                    .filter(id -> id.equals(item("/employees/1056"))
                                            || id.equals(item("/employees/1088"))
                                    )
                                    .isPresent()
                            )
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleExistentialAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SUPERVISOR, any())

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SUPERVISOR, asFrame())
                                    .isPresent()
                            )
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleNonExistentialAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(REPORT, any(NIL))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(REPORT).isEmpty())
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleMixedAnyConstraints() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(SUPERVISOR, any(
                                    NIL,
                                    item("/employees/1002")
                            ))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> {

                                final Optional<IRI> id=employee.value(SUPERVISOR, asFrame()).flatMap(Frame::id);

                                return id.isEmpty()
                                        || id.filter(iri -> iri.equals(item("/employees/1002"))).isPresent();

                            })
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testFilterOnExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(
                                    expression(List.of(), List.of(SUPERVISOR, SENIORITY)),
                                    gte(literal(integer(3)))
                            )

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(SUPERVISOR, asFrame())
                                    .flatMap(StoreTest::Employee)
                                    .flatMap(supervisor -> supervisor.value(SENIORITY, asInt()))
                                    .filter(seniority -> seniority >= 3)
                                    .isPresent()
                            )
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testFilterOnComputedExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(
                                    expression(List.of(YEAR), List.of(BIRTHDATE)),
                                    gte(literal(integer(2000)))
                            )

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.value(BIRTHDATE, asLocalDate())
                                    .filter(birthdate -> birthdate.getYear() >= 2000)
                                    .isPresent()
                            )
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testFilterOnAggregateExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(
                                    expression(List.of(COUNT), List.of(REPORT)),
                                    gte(literal(integer(3)))
                            )

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .filter(employee -> employee.values(REPORT).count() >= 3)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

    }

    @Nested
    final class Ordering {

        @Test void testSortByDefault() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testSortOnRoot() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(), List.of()), -1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(Comparator.<Frame, String>comparing(employee -> employee.id()
                                    .map(Value::stringValue)
                                    .orElseThrow()
                            ).reversed())
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testSortOnFieldIncreasing() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(RDFS.LABEL), 1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(comparing(employee -> employee
                                    .value(RDFS.LABEL, asString())
                                    .orElseThrow()
                            ))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testSortOnFieldDecreasing() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(RDFS.LABEL), -1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(Comparator.<Frame, String>comparing(employee -> employee
                                    .value(RDFS.LABEL, asString())
                                    .orElseThrow()
                            ).reversed())
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testSortOnExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(), List.of(SUPERVISOR, RDFS.LABEL)), 1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(comparing(employee -> employee
                                    .value(SUPERVISOR, asFrame())
                                    .flatMap(StoreTest::Employee)
                                    .flatMap(supervisor -> supervisor.value(RDFS.LABEL, asString()))
                                    .orElse("")
                            ))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testSortOnComputedExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(YEAR), List.of(BIRTHDATE)), 1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(comparing(employee -> employee
                                    .value(BIRTHDATE, asLocalDate())
                                    .map(LocalDate::getYear)
                                    .orElse(0)
                            ))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testSortOnAggregateExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(MAX, YEAR), List.of(REPORT, BIRTHDATE)), 1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(comparing(employee -> employee
                                    .values(REPORT, asFrame())
                                    .flatMap(report -> Employee(report).stream())
                                    .flatMap(report -> report.value(BIRTHDATE, asLocalDate()).stream())
                                    .map(LocalDate::getYear)
                                    .max(Integer::compare)
                                    .orElse(-1)
                            ))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testSortOnMultipleCriteria() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),

                            order(SENIORITY, -2),
                            order(RDFS.LABEL, +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .sorted(Comparator.<Frame, Integer>comparing(employee -> employee
                                    .value(SENIORITY, asInt())
                                    .orElseThrow()
                            ).reversed().thenComparing(comparing(employee -> employee
                                    .value(RDFS.LABEL, asString())
                                    .orElseThrow()
                            )))
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }


        @Test void testHandleRange() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),

                            offset(5),
                            limit(10)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .skip(5)
                            .limit(10)
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

        @Test void testHandleDefaultRange() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(field(ID, iri())),

                            filter(TYPE, EMPLOYEE_TYPE),

                            offset(0),
                            limit(0)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.values(RDFS.MEMBER, asFrame()))
                    .map(Frame::id)
                    .containsExactlyElementsOf(EMPLOYEES.stream()
                            .map(Frame::id)
                            .collect(toList())
                    )
            );
        }

    }

}
