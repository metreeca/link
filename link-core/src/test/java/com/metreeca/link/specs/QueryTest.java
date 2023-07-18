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

package com.metreeca.link.specs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.metreeca.link.specs.Column.column;
import static com.metreeca.link.specs.Constraint.any;
import static com.metreeca.link.specs.Criterion.increasing;
import static com.metreeca.link.specs.Specs.*;
import static com.metreeca.link.specs.Table.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class QueryTest {

    @Nested
    final class Analytics {

        @Test void testReportFilteringOnNestedProjectedValue() {

            assertThatIllegalArgumentException().isThrownBy(() -> {
                final Map<String, Column> model=Map.of("value", column("field", ""));
                final Specs[] specs=new Specs[]{ filter("value.field", any()) };
                Query.query(table(model), specs);
            });

            final Map<String, Column> model1=Map.of("value", column("field", ""));
            final Specs[] specs1=new Specs[]{ filter("abs:value", any()) };
            assertThat(Query.query(table(model1), specs1).model()).isInstanceOf(Table.class);

            final Map<String, Column> model=Map.of("value", column("field", ""));
            final Specs[] specs=new Specs[]{ filter("abs:field.value", any()) };
            assertThat(Query.query(table(model), specs).model()).isInstanceOf(Table.class);

        }

        @Test void testReportFocusingOnNestedProjectedValue() {

            assertThatIllegalArgumentException().isThrownBy(() -> {
                final Map<String, Column> model=Map.of("value", column("field", ""));
                final Specs[] specs=new Specs[]{ focus("value.field", Set.of("")) };
                Query.query(table(model), specs);
            });

            final Map<String, Column> model1=Map.of("value", column("field", ""));
            final Specs[] specs1=new Specs[]{ focus("abs:value", Set.of("")) };
            assertThat(Query.query(table(model1), specs1).model()).isInstanceOf(Table.class);

            final Map<String, Column> model=Map.of("value", column("field", ""));
            final Specs[] specs=new Specs[]{ focus("abs:field.value", Set.of("")) };
            assertThat(Query.query(table(model), specs).model()).isInstanceOf(Table.class);

        }

        @Test void testReportOrderingOnNestedProjectedValue() {

            assertThatIllegalArgumentException().isThrownBy(() -> {
                final Map<String, Column> model=Map.of("value", column("field", ""));
                final Specs[] specs=new Specs[]{ order("value.field", increasing) };
                Query.query(table(model), specs);
            });

            final Map<String, Column> model1=Map.of("value", column("field", ""));
            final Specs[] specs1=new Specs[]{ order("abs:value", increasing) };
            assertThat(Query.query(table(model1), specs1).model()).isInstanceOf(Table.class);

            final Map<String, Column> model=Map.of("value", column("field", ""));
            final Specs[] specs=new Specs[]{ order("abs:field.value", increasing) };
            assertThat(Query.query(table(model), specs).model()).isInstanceOf(Table.class);

        }

    }

}
