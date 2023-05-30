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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.link.Frame.with;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.Stash.integer;
import static com.metreeca.link.Table.column;
import static com.metreeca.link.Table.table;
import static com.metreeca.link.rdf4j.RDF4J.rdf4j;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

final class RDF4JTest extends EngineTest {

    static {
        final Logger logger=Logger.getLogger("com.metreeca");
        logger.setLevel(Level.ALL);
        logger.addHandler(with(new ConsoleHandler(), handler -> handler.setLevel(Level.ALL)));
    }


    @Override protected Engine engine() {
        return rdf4j(new SailRepository(new MemoryStore()));
    }


    @Nested
    final class Projecting {

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
    final class Filtering {

    }

    @Nested
    final class Sorting {

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

    @Test void test() {

        assertThat(testbed().retrieve(with(new Employees(), employees -> {

            employees.setId(id("/employees/"));
            employees.setMembers(query(
                    model(table(Map.of(
                            "office", column("office", ""),
                            "employees", column("count:", integer(0))
                    )))
            ));

        }))).hasValueSatisfying(employees -> assertThat(((Table<?>)employees.getMembers()).records())

                .isEqualTo(Employees.stream()

                        .collect(groupingBy(Employee::getOffice))
                        .entrySet().stream()

                        .map(e -> Map.of(
                                "office", e.getKey(),
                                "employees", integer(e.getValue().size())
                        ))

                        .collect(toList())
                )

        );

    }

}