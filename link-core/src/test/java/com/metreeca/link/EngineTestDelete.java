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

public abstract class EngineTestDelete {

    protected abstract Engine testbed();
    

    @Test void testDelete() {

        final Engine engine=testbed();

        final Employee delete=with(new Employee(), o -> o.setId(id("/employees/1702")));

        assertThat(engine.delete(delete))
                .hasValueSatisfying(employee -> assertThat(employee.getId())
                        .isEqualTo(delete.getId())
                );

        assertThat(engine.retrieve(with(new Employee(), employee -> employee.setId(delete.getId()))))
                .isEmpty();


    }


    @Test void testReportUnknownResources() {

        final Engine engine=testbed();

        final Employee delete=with(new Employee(), office -> office.setId(id("/employees/999")));

        assertThat(engine.delete(delete))
                .isEmpty();

    }

    @Test void testReportMissingId() {

        final Engine engine=testbed();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.delete(new Employee()));
    }

}
