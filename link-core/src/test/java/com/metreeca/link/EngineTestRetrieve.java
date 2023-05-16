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

import static com.metreeca.link.EngineTest.id;
import static com.metreeca.link.Frame.with;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public abstract class EngineTestRetrieve {

    protected abstract Engine testbed();


    @Test void testRetrieveResource() {

        assertThat(testbed().retrieve(with(new Employee(), employee -> {

            employee.setId(id("/employees/1702"));
            employee.setLabel("");
            employee.setSeniority(0);

        }))).hasValueSatisfying(employee -> {

            // specified by model

            assertThat(employee.getLabel()).isEqualTo("Martin Gerard");
            assertThat(employee.getSeniority()).isEqualTo(2);

            // not specified by model

            assertThat(employee.getSurname()).isNull();
            assertThat(employee.getSupervisor()).isNull();
            assertThat(employee.getReports()).isNull();

        });

    }

    @Test void testRetrieveNestedResource() {

        assertThat(testbed().retrieve(with(new Employee(), employee -> {

            employee.setId(id("/employees/1702"));
            employee.setSupervisor(with(new Employee(), supervisor -> {

                supervisor.setLabel("");
                supervisor.setSeniority(0);

            }));

        })).map(Employee::getSupervisor)).hasValueSatisfying(supervisor -> {

            assertThat(supervisor.getLabel()).isEqualTo("Gerard Bondur");
            assertThat(supervisor.getSeniority()).isEqualTo(4);

        });

    }


    @Test void testReportUnknownResources() {

        assertThat(testbed().retrieve(with(new Employee(), employee -> {

            employee.setId(id("/employees/999"));
            employee.setLabel("Memmo Cancelli");

        }))).isEmpty();

    }

    @Test void testReportMissingId() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> testbed().retrieve(new Employee()));

    }

}
