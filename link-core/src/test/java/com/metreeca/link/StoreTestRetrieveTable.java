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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Probe.probe;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.StoreTest.*;
import static com.metreeca.link.Transform.*;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public abstract class StoreTestRetrieveTable {

    protected abstract Store store();


    private Collection<Object> object(final Stream<? extends Value> values) {
        return values
                .map(this::object)
                .collect(toList());
    }

    private Object object(final Value value) {
        if ( value instanceof Frame ) {

            return ((Frame)value).fields().entrySet().stream().collect(toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream().map(this::object).collect(toList())
            ));

        } else {

            return value;

        }
    }


    @Nested
    final class Projecting {

        @Test void testProjectPlainTable() {

            final Probe e=probe("employee", expression(RDFS.LABEL));
            final Probe s=probe("supervisor", expression(SUPERVISOR, RDFS.LABEL));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(e, literal("")),
                                    field(s, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(comparing((Frame employee) -> employee.value(RDFS.LABEL, asString()).orElse(""))
                                    .thenComparing((Frame employee) -> employee.value(SUPERVISOR, asFrame())
                                            .flatMap(StoreTest::Employee)
                                            .flatMap(supervisor -> supervisor.value(RDFS.LABEL, asString()))
                                            .orElse("")
                                    )
                            )

                            .map(employee -> frame(

                                    field(e, employee.value(RDFS.LABEL)),
                                    field(s, employee.value(SUPERVISOR, asFrame())
                                            .flatMap(StoreTest::Employee)
                                            .flatMap(supervisor -> supervisor.value(RDFS.LABEL))
                                            .orElse(NIL)
                                    )

                            ))

                    ))

            );
        }

        @Test void testProjectTotalTable() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(Stream.of(frame(

                            field(v, literal(integer(EMPLOYEES.size())))

                    ))))

            );

        }

        @Test void testProjectGroupedTable() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(SENIORITY, literal(integer(0))),
                                    field(v, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .collect(groupingBy(

                                    employee -> employee.value(SENIORITY, asInteger()).orElse(BigInteger.ZERO),
                                    counting()

                            ))

                            .entrySet().stream()

                            .sorted(comparingByKey())

                            .map(entry -> frame(
                                    field(SENIORITY, literal(entry.getKey())),
                                    field(v, literal(integer(entry.getValue())))
                            ))

                    ))

            );
        }

        @Test void testProjectEmptyResultSet() {

            final Probe v=probe("value", expression(RDFS.LABEL));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(RDFS.LABEL, like("none"))


                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEmpty()

            );

        }

        @Test void testProjectNestedModel() {

            final Probe e=probe("employee", expression());
            final Probe v=probe("value", expression(List.of(COUNT), List.of(REPORT)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(e, frame(
                                            field(RDFS.LABEL, literal("")),
                                            field(OFFICE, frame(
                                                    field(RDFS.LABEL, literal(""))
                                            ))
                                    )),
                                    field(v,
                                            literal(integer(0))
                                    )
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .map(employee -> frame(
                                    field(e, frame(
                                            field(RDFS.LABEL, employee.value(RDFS.LABEL)),
                                            field(OFFICE, employee.value(OFFICE, asFrame())
                                                    .flatMap(StoreTest::Office)
                                                    .map(office -> frame(
                                                            field(RDFS.LABEL, office.value(RDFS.LABEL))
                                                    ))
                                            )
                                    )),
                                    field(v, literal(integer(employee.values(REPORT).count())))
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Computing {

        @Test void testComputeAbs() {

            final Probe v=probe("value", expression(List.of(ABS), List.of(DELTA)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v,
                                            literal(decimal(0))
                                    )
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .map(employee -> frame(
                                    field(v, employee.value(DELTA, asDecimal())
                                            .map(BigDecimal::abs)
                                            .map(Frame::literal)
                                            .map(Value.class::cast)
                                            .orElse(NIL)
                                    )
                            ))

                            .sorted(comparing(
                                    tuple -> tuple.value(v, asDecimal()).orElse(null),
                                    nullsFirst(BigDecimal::compareTo)
                            ))

                    ))

            );
        }

        @Test void testComputeRound() {

            final Probe v=probe("value", expression(List.of(ROUND), List.of(DELTA)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v,
                                            literal(decimal(0))
                                    )
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .map(employee -> frame(
                                    field(v, employee.value(DELTA, asDecimal())
                                            .map(value -> value.setScale(0, HALF_UP))
                                            .map(Frame::literal)
                                            .map(Value.class::cast)
                                            .orElse(NIL))
                            ))

                            .sorted(comparing(
                                    tuple -> tuple.value(v, asDecimal()).orElse(null),
                                    nullsFirst(BigDecimal::compareTo)
                            ))

                    ))

            );
        }

        @Test void testComputeYear() {

            final Probe v=probe("value", expression(List.of(YEAR), List.of(BIRTHDATE)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v,
                                            literal(decimal(0))
                                    )
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .map(employee -> frame(
                                    field(v, employee.value(BIRTHDATE, asTemporalAccessor())
                                            .map(value -> integer(value.get(ChronoField.YEAR)))
                                            .map(Frame::literal)
                                    )
                            ))

                            .sorted(comparing(
                                    tuple -> tuple.value(v, asDecimal()).orElse(null),
                                    nullsFirst(BigDecimal::compareTo)
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Aggregating {

        @Test void testComputeCountDistinct() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of(OFFICE)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(decimal(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(Stream.of(frame(
                            field(v, literal(integer(EMPLOYEES.stream()
                                    .flatMap(employee -> employee.values(OFFICE, asFrame()))
                                    .flatMap(office -> office.id().stream())
                                    .distinct()
                                    .count()
                            )))
                    ))))

            );
        }

        @Test void testComputeMin() {

            final Probe v=probe("value", expression(List.of(MIN), List.of(YTD)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(decimal(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.value(RDFS.MEMBER, asFrame())
                            .flatMap(frame -> frame.value(v, asDouble()))
                            .orElse(Double.NaN)
                    )

                            .isEqualTo(
                                    EMPLOYEES.stream()
                                            .flatMap(employee -> employee.values(YTD, asDecimal()))
                                            .mapToDouble(BigDecimal::doubleValue)
                                            .min()
                                            .orElse(Double.NaN),
                                    offset(1.0E-6)
                            )

            );
        }

        @Test void testComputeMax() {

            final Probe v=probe("value", expression(List.of(MAX), List.of(YTD)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(decimal(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.value(RDFS.MEMBER, asFrame())
                            .flatMap(frame -> frame.value(v, asDouble()))
                            .orElse(Double.NaN)
                    )

                            .isEqualTo(
                                    EMPLOYEES.stream()
                                            .flatMap(employee -> employee.values(YTD, asDecimal()))
                                            .mapToDouble(BigDecimal::doubleValue)
                                            .max()
                                            .orElse(Double.NaN),
                                    offset(1.0E-6)
                            )

            );
        }

        @Test void testComputeSum() {

            final Probe v=probe("value", expression(List.of(SUM), List.of(YTD)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(decimal(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(Stream.of(frame(
                            field(v, literal(decimal(EMPLOYEES.stream()
                                    .flatMap(employee -> employee.values(YTD, asDecimal()))
                                    .mapToDouble(BigDecimal::doubleValue)
                                    .sum()
                            )))
                    ))))

            );
        }

        @Test void testComputeAvg() {

            final Probe v=probe("value", expression(List.of(AVG), List.of(YTD)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(decimal(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(employees.value(RDFS.MEMBER, asFrame())
                            .flatMap(frame -> frame.value(v, asDouble()))
                            .orElse(Double.NaN)
                    )

                            .isEqualTo(
                                    EMPLOYEES.stream()
                                            .flatMap(employee -> employee.values(YTD, asDecimal()))
                                            .mapToDouble(BigDecimal::doubleValue)
                                            .average()
                                            .orElse(Double.NaN),
                                    offset(1.0E-6)
                            )

            );
        }

    }

    @Nested
    final class Grouping {

        @Test void testGroupOnPlainExpression() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(SENIORITY, literal(integer(0))),
                                    field(v, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .collect(groupingBy(

                                    employee -> employee.value(SENIORITY, asInteger()).orElse(BigInteger.ZERO),
                                    counting()

                            ))

                            .entrySet().stream()

                            .sorted(comparingByKey())

                            .map(entry -> frame(
                                    field(SENIORITY, literal(entry.getKey())),
                                    field(v, literal(integer(entry.getValue())))
                            ))

                    ))

            );
        }

        @Test void testGroupOnComputedExpression() {

            final Probe x=probe("x", expression(List.of(YEAR), List.of(BIRTHDATE)));
            final Probe y=probe("y", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(x, literal(integer(0))),
                                    field(y, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .collect(groupingBy(

                                    employee -> employee.value(BIRTHDATE, asTemporalAccessor())
                                            .map(v -> integer(v.get(ChronoField.YEAR)))
                                            .orElse(BigInteger.ZERO),

                                    counting()

                            ))

                            .entrySet().stream()

                            .sorted(comparingByKey())

                            .map(entry -> frame(
                                    field(x, literal(entry.getKey())),
                                    field(y, literal(integer(entry.getValue())))
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Filtering {

        @Test void testFilterOnPlainExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(expression(REPORT, DELTA), lte(literal(integer(-100_000))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .filter(employee -> employee.values(REPORT, asFrame())
                                    .flatMap(report -> Employee(report).stream())
                                    .flatMap(report -> report.value(DELTA, asDouble()).stream())
                                    .mapToDouble(value -> value)
                                    .anyMatch(delta -> delta <= -100_000)
                            )

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testFilterOnComputedExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(expression(List.of(AVG, YEAR), List.of(REPORT, BIRTHDATE)), gte(literal(integer(1995))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .filter(employee -> employee.values(REPORT, asFrame())
                                    .flatMap(report -> Employee(report).stream())
                                    .flatMap(report -> report.value(BIRTHDATE, asTemporalAccessor()).stream())
                                    .mapToInt(value -> value.get(ChronoField.YEAR))
                                    .average()
                                    .orElse(Double.NaN) >= 1995
                            )

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testFilterOnAggregateExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(expression(List.of(COUNT), List.of(REPORT)), gte(literal(integer(2))))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .filter(employee -> employee.values(REPORT).count() >= 2)

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testFilteringIgnoresProjectedProperties() {

            final Probe v=probe("value", expression(SURNAME));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            filter(RDFS.LABEL, like("mary"))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .filter(employee -> employee.values(RDFS.LABEL, asString())
                                    .anyMatch(label -> label.toLowerCase(Locale.ROOT).contains("mary"))
                            )

                            .map(employee -> frame(
                                    field(v, employee.value(SURNAME))
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Ordering {

        @Test void testOrderOnPlainExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(DELTA, +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(comparing(
                                    frame -> frame.value(DELTA, asDouble()).orElse(null),
                                    nullsFirst(Double::compareTo)
                            ))

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testOrderOnComputedExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(YEAR), List.of(BIRTHDATE)), +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(comparing(frame -> frame.value(BIRTHDATE, asTemporalAccessor())
                                    .map(v -> v.get(ChronoField.YEAR))
                                    .orElseThrow()
                            ))

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testOrderOnAggregateExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(expression(List.of(COUNT), List.of(REPORT)), +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(comparing(frame -> frame.values(REPORT).count()))

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testOrderingIgnoresProjectedProperties() {

            final Probe v=probe("value", expression(SURNAME));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            order(RDFS.LABEL, +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(comparing(frame -> frame.value(RDFS.LABEL, asString()).orElse("")))

                            .map(employee -> frame(
                                    field(v, employee.value(SURNAME))
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Focusing {

        @Test void testFocusOnPlainExpression() {
            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(RDFS.LABEL, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            focus(RDFS.LABEL, Set.of(literal("Mary Patterson")))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(
                                    comparing((Frame frame) -> frame.value(RDFS.LABEL, asString())
                                            .filter("Mary Patterson"::equals)
                                            .isPresent()
                                    ).reversed()
                            )

                            .map(employee -> frame(
                                    field(RDFS.LABEL, employee.value(RDFS.LABEL))
                            ))

                    ))

            );
        }

        @Test void testFocusingIgnoresProjectedProperties() {

            final Probe v=probe("value", expression(SURNAME));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(""))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),
                            focus(RDFS.LABEL, Set.of(literal("Mary Patterson")))

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .sorted(
                                    comparing((Frame frame) -> frame.value(RDFS.LABEL, asString())
                                            .filter("Mary Patterson"::equals)
                                            .isPresent()
                                    ).reversed()

                                            .thenComparing(frame -> frame.value(SURNAME, asString()).orElse(""))
                            )

                            .map(employee -> frame(
                                    field(v, employee.value(SURNAME))
                            ))

                    ))

            );
        }

    }

    @Nested
    final class Analyzing {

        @Test void testAnalyzeStats() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(v, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(Stream.of(frame(
                            field(v, literal(integer(EMPLOYEES.size())))
                    ))))

            );
        }

        @Test void testAnalyzeOptions() {

            final Probe v=probe("value", expression(List.of(COUNT), List.of()));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(OFFICE, frame(
                                            field(ID, iri()),
                                            field(RDFS.LABEL, literal(""))
                                    )),
                                    field(v, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE),

                            order(expression(List.of(COUNT), List.of()), -3),
                            // !!! order(expression(OFFICE, RDFS.LABEL), +2),
                            order(expression(OFFICE), +1)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(EMPLOYEES.stream()

                            .collect(groupingBy(employee -> employee.value(OFFICE, asFrame())
                                    .flatMap(Frame::id)
                                    .orElseThrow()
                            ))

                            .entrySet().stream()

                            .flatMap(e -> Office(e.getKey()).map(frame -> frame(
                                    field(OFFICE, frame(
                                            field(ID, e.getKey()),
                                            field(RDFS.LABEL, frame.value(RDFS.LABEL))
                                    )),
                                    field(v, literal(integer(e.getValue().size())))
                            )).stream())

                            .sorted(Comparator.comparing((Frame frame) -> frame.value(v, asLong()).orElseThrow()).reversed()
                                    // !!! .thenComparing(v -> v.value(OFFICE, asFrame()).flatMap(frame -> frame.value(RDFS.LABEL, asString())).orElseThrow())
                                    .thenComparing(frame -> frame.value(OFFICE, asFrame()).flatMap(Frame::id).map(Value::stringValue).orElseThrow())
                            )

                    ))

            );
        }

        @Test void testAnalyzeRange() {

            final Probe min=probe("min", expression(List.of(MIN), List.of(SENIORITY)));
            final Probe max=probe("max", expression(List.of(MAX), List.of(SENIORITY)));

            assertThat(populate(store()).retrieve(item("/employees/"), Employees(), frame(

                    field(RDFS.MEMBER, query(

                            frame(
                                    field(min, literal(integer(0))),
                                    field(max, literal(integer(0)))
                            ),

                            filter(TYPE, EMPLOYEE_TYPE)

                    ))

            ))).hasValueSatisfying(employees -> assertThat(object(employees.values(RDFS.MEMBER, asFrame())))

                    .isEqualTo(object(Stream.of(frame(
                            field(min, EMPLOYEES.stream()
                                    .flatMap(e -> e.values(SENIORITY, asInt()))
                                    .mapToInt(value -> value)
                                    .min()
                                    .stream()
                                    .mapToObj(v -> literal(integer(v)))
                            ),
                            field(max, EMPLOYEES.stream()
                                    .flatMap(e -> e.values(SENIORITY, asInt()))
                                    .mapToInt(value -> value)
                                    .max()
                                    .stream()
                                    .mapToObj(v -> literal(integer(v)))
                            )
                    ))))

            );
        }

    }

}
