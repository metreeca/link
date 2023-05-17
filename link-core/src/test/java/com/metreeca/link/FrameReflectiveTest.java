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

import com.metreeca.link.jsonld.Id;
import com.metreeca.link.jsonld.Property;
import com.metreeca.link.jsonld.Reverse;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class FrameReflectiveTest {

    @Nested final class Ids {

        @Test void testGetId() {

            final Bean bean=new Bean();
            final Frame<Bean> frame=new FrameReflective<>(bean);

            assertThat(frame.id()).isNull();

            bean.setId("test");

            assertThat(frame.id()).contains(bean.getId());

        }

        @Test void testSetId() {

            final Bean test=new Bean();

            new FrameReflective<>(test).id("test");

            assertThat(test.getId()).isEqualTo("test");

        }

        @Test void testReportIllegalMultipleIds() {
            assertThatIllegalArgumentException().isThrownBy(() -> new FrameReflective<>(BeanMultipleIds.class));
        }

        @Test void testReportIllegalIdType() {
            assertThatIllegalArgumentException().isThrownBy(() -> new FrameReflective<>(BeanIllegalId.class));
        }

        @Test void testReportIdOnProperty() {
            assertThatIllegalArgumentException().isThrownBy(() -> new FrameReflective<>(BeanIdOnProperty.class));
        }

        @Test void testReportIdOnReverse() {
            assertThatIllegalArgumentException().isThrownBy(() -> new FrameReflective<>(BeanIdOnReverse.class));
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Bean {

        @Id
        private String id;


        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id=id;
        }

    }

    public static final class BeanMultipleIds {

        @Id
        private Object x;

        @Id
        private Object y;

        public Object getX() {
            return x;
        }

        public Object getY() {
            return y;
        }

    }

    public static final class BeanIllegalId {

        @Id
        private Object id;

        public Object getId() {
            return id;
        }

    }

    public static final class BeanIdOnProperty {

        @Id
        @Property("property")
        private Object id;

        public Object getId() {
            return id;
        }

    }

    public static final class BeanIdOnReverse {

        @Id
        @Reverse
        private Object id;

        public Object getId() {
            return id;
        }

    }

}