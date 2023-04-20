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

import org.junit.jupiter.api.Test;

import static com.metreeca.link.Shape.shape;

import static org.assertj.core.api.Assertions.assertThat;

final class ShapeTest {

    public static final class Recursive {

        public Recursive getRecursive() { return null; }

    }


    @Test void testHandleRecursiveDefinitions() {
        assertThat(shape(Recursive.class).shape("recursive"))
                .flatMap(Shape::clazz)
                .contains(Recursive.class);
    }


    // !!! report maxCount > 1 on scalars
    // !!! report minCount > 1 on scalars

}