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

package com.metreeca.rest.json;

import com.metreeca.rest.Query;
import com.metreeca.rest.Table;
import com.metreeca.rest.jsonld.Id;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;

import static com.metreeca.rest.Query.Constraint.*;
import static com.metreeca.rest.Query.Direction.decreasing;
import static com.metreeca.rest.Query.Direction.increasing;
import static com.metreeca.rest.Stash.Expression.expression;
import static com.metreeca.rest.Table.Column.column;
import static com.metreeca.rest.json.JSONTest.decode;
import static com.metreeca.rest.json.JSONTest.encode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Map.entry;

final class TypeObjectTest {

    private static Items items() {

        final Items items=new Items();

        final Item one=new Item();
        final Item two=new Item();

        one.setId("/items/1");
        one.setLabel("one");

        two.setId("/items/2");
        two.setLabel("two");

        final Set<Item> members=new LinkedHashSet<>();

        members.add(one);
        members.add(two);

        items.setMembers(members);
        return items;
    }

    private static String json() {
        return "{\"id\":\"/items/\",\"members\":["
                +"{\"id\":\"/items/1\",\"label\":\"one\"},{\"id\":\"/items/2\",\"label\":\"two\"}"
                +"]}";
    }


    @Nested final class Encode {

        @Test void testEncodeBean() {
            assertThat(encode(items())).isEqualTo(json());
        }

    }

    @Nested final class Decode {

        @Test void testDecodeBean() {

            final Items actual=decode(json(), Items.class);
            final Items expected=items();

            assertThat(actual.getId()).isEqualTo(expected.getId());

            final List<Item> actualMembers=new ArrayList<>(actual.getMembers());
            final List<Item> expectedMembers=new ArrayList<>(expected.getMembers());

            assertThat(actualMembers).size().isEqualTo(expectedMembers.size());

            for (int i=0; i < actualMembers.size(); ++i) {

                final Item actualMember=actualMembers.get(i);
                final Item expectedMember=expectedMembers.get(i);

                assertThat(actualMember.getId()).isEqualTo(expectedMember.getId());
                assertThat(actualMember.getLabel()).isEqualTo(expectedMember.getLabel());

            }

        }


        @Test void testReportUnexpectedQuery() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{ \"related\": { \"#\":  0 } }", Item.class));
        }

    }

    @Nested final class Queries {

        private Query<?> query(final String query) {
            return Optional.of(decode("{ \"members\" :  ["+query+"] }", Items.class).getMembers())
                    .filter(Query.class::isInstance)
                    .map(Query.class::cast)
                    .orElseGet(Query::query);
        }


        @Test void testDecodeFrameTemplate() {
            assertThat(query("{ \"label\": \"value\", \"~label\": \"keywords\" }").template())
                    .isInstanceOf(Item.class)
                    .extracting(Item.class::cast)
                    .satisfies(item -> assertThat(item.getLabel())
                            .isEqualTo("value")
                    );
        }

        @Test void testDecodeTableTemplate() {
            assertThat(query("{ \"alias=label\": \"\" }").template())
                    .isInstanceOf(Table.class)
                    .extracting(v -> (Table<?>)v)
                    .satisfies(table -> assertThat(table.columns())
                            .containsExactly(
                                    entry("alias", column(expression("label"), ""))
                            )
                    );
        }

        @Test void testDecodeMixedTemplate() {
            assertThat(query("{ \"label\": \"\", \"alias=label\": \"\" }").template())
                    .isInstanceOf(Table.class)
                    .extracting(v -> (Table<?>)v)
                    .satisfies(table -> assertThat(table.columns())
                            .containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                                    entry("label", column(expression("label"), "")),
                                    entry("alias", column(expression("label"), ""))
                            ))
                    );
        }


        @Test void testLookupTableShapes() {
            assertThat(query("{ \"alias=item\": { \"label\":  \"\"} }").template())
                    .isInstanceOf(Table.class)
                    .extracting(v -> (Table<?>)v)
                    .satisfies(table -> assertThat(table.columns().get("alias").template())
                            .isInstanceOf(Item.class)
                            .extracting(Item.class::cast)
                            .satisfies(item -> assertThat(item.getLabel())
                                    .isEqualTo("")
                            )
                    );
        }

        @Test void testLookupTableNestedShapes() {
            assertThat(query("{ \"alias=item.item\": { \"label\":  \"\"} }").template())
                    .isInstanceOf(Table.class)
                    .extracting(v -> (Table<?>)v)
                    .satisfies(table -> assertThat(table.columns().get("alias").template())
                            .isInstanceOf(Item.class)
                            .extracting(Item.class::cast)
                            .satisfies(item -> assertThat(item.getLabel())
                                    .isEqualTo("")
                            )
                    );
        }

        @Test void testIgnoreTableComputedShapes() {
            assertThat(query("{ \"alias=max:items\": { \"label\":  \"\"} }").template())
                    .isInstanceOf(Table.class)
                    .extracting(v -> (Table<?>)v)
                    .satisfies(table -> assertThat(table.columns().get("alias").template())
                            .isInstanceOf(Map.class)
                            .extracting(v -> (Map<?, ?>)v)
                            .satisfies(map -> assertThat(map)
                                    .isEqualTo(Map.of("label", ""))
                            )
                    );
        }


        @Test void testDecodeLtConstraint() {
            assertThat(query("{ \"<field\": \"value\" }").filters())
                    .containsExactly(entry(expression("field"), lt("value")));
        }

        @Test void testDecodeGtConstraint() {
            assertThat(query("{ \">field\": \"value\" }").filters())
                    .containsExactly(entry(expression("field"), gt("value")));
        }

        @Test void testDecodeLteConstraint() {
            assertThat(query("{ \"<=field\": \"value\" }").filters())
                    .containsExactly(entry(expression("field"), lte("value")));
        }

        @Test void testDecodeGteConstraint() {
            assertThat(query("{ \">=field\": \"value\" }").filters())
                    .containsExactly(entry(expression("field"), gte("value")));
        }

        @Test void testDecodeLikeConstraint() {
            assertThat(query("{ \"~field\": \"value\" }").filters())
                    .containsExactly(entry(expression("field"), like("value")));
        }

        @Test void testReportMalformedLikeConstraint() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"~field\": 0 }"));
        }

        @Test void testDecodeAnyConstraint() {

            assertThat(query("{ \"?field\": [] }").filters())
                    .containsExactly(entry(expression("field"), any()));

            assertThat(query("{ \"?field\": [null, true, 1, \"value\"] }").filters())
                    .containsExactly(entry(expression("field"), any(null, true, BigInteger.ONE, "value")));
        }

        @Test void testReportUnknownConstraints() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"±field\": 0 }"));
        }


        @Test void testDecodeOrder() {
            assertThat(query("{ \"^\": { \"y\": \"increasing\", \"x\": \"decreasing\" } }").order().entrySet())
                    .containsExactly(
                            entry(expression("y"), increasing),
                            entry(expression("x"), decreasing)
                    );
        }

        @Test void testReportMalformedOrder() {

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"^\": 0 }"));

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"^\": { \"y\": 0 } }"));

        }


        @Test void testDecodeOffset() {
            assertThat(query("{ \"@\": 100  }").offset())
                    .isEqualTo(100);
        }

        @Test void testReportMalformedOffset() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"@\": true  }"));
        }


        @Test void testDecodeLimit() {
            assertThat(query("{ \"#\": 100  }").limit())
                    .isEqualTo(100);
        }

        @Test void testReportMalformedLimit() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"#\": true  }"));
        }


        @Test void testReportUnexpectedScalarValueType() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \">=integer\": \"100\"  }"));

        }

        @Test void testReportUnexpectedCollectionValueType() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ \"?integer\": [\"100\"]  }"));

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract static class Resource {

        @Id
        private String id;


        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id=id;
        }

    }

    public abstract static class Container<T extends Resource> extends Resource {

        private Set<T> members;


        public Set<T> getMembers() {
            return members;
        }

        public void setMembers(final Set<T> members) { // !!! Collection<T>
            this.members=members;
        }

    }

    public static final class Items extends Container<Item> {

        public Items() {
            setId("/items/");
        }

    }

    public static final class Item extends Resource {

        private String label;

        private Item item;

        private Integer integer;
        private Set<Integer> integers;


        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label=label;
        }


        public Item getItem() {
            return item;
        }

        public void setItem(final Item item) {
            this.item=item;
        }


        public Integer getInteger() {
            return integer;
        }

        public void setInteger(final Integer integer) {
            this.integer=integer;
        }


        public Set<Integer> getIntegers() {
            return integers;
        }

        public void setIntegers(final Set<Integer> integers) {
            this.integers=integers;
        }

    }

}