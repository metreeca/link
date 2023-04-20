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

import com.metreeca.link.Shape;
import com.metreeca.link.toys.Employee;

import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.metreeca.link.Shape.shape;

import static java.util.stream.Collectors.toList;

final class ShapeCodecTest {

    @Test void test() {

        final Shape shape=shape(Employee.class);

        final List<Statement> model=new com.metreeca.link.rdf4j.ShapeCodec().encode(shape).collect(toList());
    }

}