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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.metreeca.link.Glass.glass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class GlassTest {

    private static final class Bean {

        private String string;
        private Object object;


        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string=string;
        }


        public Object getObject() {
            return object;
        }

        public void setObject(final Object object) {
            this.object=object;
        }

    }


    @Test void testScanProperties() {
        assertThat(glass(Bean.class).properties())
                .extracting(Map::keySet)
                .isEqualTo(Set.of("string", "object"));
    }

    // !!! set null >> primitive default value


    @Test void testHandleCovariantGetters() {

        class Base {

            public Object getValue() { return "base"; }

        }

        class Bean extends Base {

            @Override public String getValue() { return "bean"; }

        }

        assertThat(glass(Bean.class).properties())
                .anySatisfy((field, property) -> {

                    assertThat(field).isEqualTo("value");
                    assertThat(property.<String>get(new Bean())).isEqualTo("bean");

                });

    }

    @Test void testHandleOverriddenSetters() {

        class Base {

            private String value;


            public String getValue() { return value; }

            public void setValue(final String value) { this.value=value; }

        }

        class Bean extends Base {

            @Override public void setValue(final String value) { super.setValue("bean/"+value); }

        }

        assertThat(glass(Bean.class).properties())
                .anySatisfy((field, property) -> {

                    final Bean bean=new Bean();

                    property.set(bean, "test");

                    assertThat(field).isEqualTo("value");
                    assertThat(property.<String>get(bean)).isEqualTo("bean/test");

                });
    }

    @Test void testHandleOverloadedSetters() {

        class Bean {

            private String value;


            public String getValue() { return value; }


            public void setValue(final String value) { this.value="string/"+value; }

            public void setValue(final Object value) { this.value="object/"+value; }

        }

        assertThat(glass(Bean.class).properties())
                .anySatisfy((field, property) -> {

                    final Bean bean=new Bean();

                    property.set(bean, "test");

                    assertThat(field).isEqualTo("value");
                    assertThat(property.<String>get(bean)).isEqualTo("object/test");

                });
    }

    @Test void testReportUnrelatedSetters() {

        class Base {

            public void setValue(final Base value) { }

        }

        class Bean extends Base {

            public void setValue(final String value) { }

        }

        assertThatIllegalArgumentException()
                .isThrownBy(() -> glass(Bean.class));
    }

    @Test void testReportInconsistentAccessors() {

        class Broken {

            public boolean isBroken() { return true; }

            public String getBroken() { return "broken"; }

        }

        assertThatIllegalArgumentException()
                .isThrownBy(() -> glass(Broken.class));
    }


    @Nested final class Collections {

        private final class ConcreteLevel0 {

            public List<String> getPayload() { return List.of(); }

        }

        private abstract class AbstractLevel0<W, T, X> {

            public List<T> getPayload() { return List.of(); }

        }

        private final class ConcreteLevel1 extends AbstractLevel0<Object, String, Object> { }


        private abstract class AbstractLevel1<Y, U, Z> extends AbstractLevel0<Y, U, Z> { }

        private final class ConcreteLevel2 extends AbstractLevel1<Object, String, Object> { }


        @Test void testLevel0() {
            assertThat(glass(ConcreteLevel0.class))
                    .extracting(glass -> glass.properties().get("payload").item())
                    .isEqualTo(Optional.of(String.class));
        }

        @Test void testLevel1() {
            assertThat(glass(ConcreteLevel1.class))
                    .extracting(glass -> glass.properties().get("payload").item())
                    .isEqualTo(Optional.of(String.class));
        }

        @Test void testLevel2() {
            assertThat(glass(ConcreteLevel2.class))
                    .extracting(glass -> glass.properties().get("payload").item())
                    .isEqualTo(Optional.of(String.class));
        }

    }

}