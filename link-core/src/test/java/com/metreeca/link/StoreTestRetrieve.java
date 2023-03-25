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


import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Frame.*;
import static com.metreeca.link.StoreTest.*;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class StoreTestRetrieve {

    // !!! virtual model merging

    protected abstract Store store();


    @Test void testIgnoreUnknownIds() {

        assertThat(store().retrieve(item("/employees/999"), Employee(), frame(

                field(RDFS.LABEL, literal(""))

        ))).isEmpty();

    }

    @Test void testRetrieveFrames() {

        assertThat(populate(store()).retrieve(item("/employees/1702"), Employee(), frame(

                field(RDFS.LABEL, literal("")),
                field(CODE, literal("")),
                field(SENIORITY, literal(integer(0)))

        ))).hasValueSatisfying(employee -> {

            // specified by model

            assertThat(employee.value(RDFS.LABEL)).contains(literal("Martin Gerard"));
            assertThat(employee.value(CODE)).contains(literal("1702"));
            assertThat(employee.value(SENIORITY)).contains(literal(integer(2)));

            // not specified by model

            assertThat(employee.value(SURNAME)).isEmpty();
            assertThat(employee.value(SUPERVISOR)).isEmpty();
            assertThat(employee.value(REPORT)).isEmpty();

        });

    }

    @Test void testRetrieveNestedFrames() {

        assertThat(populate(store()).retrieve(item("/employees/1702"), Employee(), frame(

                field(SUPERVISOR, frame(
                        field(RDFS.LABEL, literal("")),
                        field(SENIORITY, literal(integer(0)))
                ))

        )))
                .flatMap(frame -> frame.value(SUPERVISOR))
                .map(Frame.class::cast)

                .hasValueSatisfying(supervisor -> {

                    assertThat(supervisor.value(RDFS.LABEL)).contains(literal("Gerard Bondur"));
                    assertThat(supervisor.value(SENIORITY)).contains(literal(integer(4)));

                });

    }

}
