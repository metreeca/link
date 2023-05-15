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

    protected abstract Engine engine();


    @Test void testRetrieveResource() {

        final Engine engine=engine();

        final Employee model=with(new Employee(), employee -> {

            employee.setId(id("/employees/1702"));
            employee.setLabel("");
            employee.setSeniority(0);

        });

        assertThat(engine.retrieve(model)).hasValueSatisfying(employee -> {

            // specified by model

            assertThat(employee.getLabel()).isEqualTo("Martin Gerard");
            assertThat(employee.getSeniority()).isEqualTo(2);

            // not specified by model

            assertThat(employee.getSurname()).isNull();
            assertThat(employee.getSupervisor()).isNull();
            assertThat(employee.getReports()).isNull();

        });

    }


    @Test void testReportUnknownResources() {

        final Engine engine=engine();

        final Employee model=with(new Employee(), employee -> {

            employee.setId(id("/employees/999"));
            employee.setLabel("Memmo Cancelli");

        });

        assertThat(engine.retrieve(model))
                .isEmpty();

    }

    @Test void testReportMissingId() {

        final Engine engine=engine();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.retrieve(new Employee()));

    }

}
