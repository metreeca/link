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

package com.metreeca.rest.rdf4j;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.rdf4j.Coder.text;

final class CoderTest {

    private String code(final CharSequence code) {
        return text(code).toString();
    }


    @Test void testCollapseFeeds() {
        Assertions.assertThat(code("x\fy")).isEqualTo("x\n\ny");
        Assertions.assertThat(code("x\n\f\n\fy")).isEqualTo("x\n\ny");
    }

    @Test void testCollapseFolds() {
        Assertions.assertThat(code("x\ry")).isEqualTo("x y");
        Assertions.assertThat(code("x\r\r\ry")).isEqualTo("x y");
        Assertions.assertThat(code("x\n\ry")).isEqualTo("x\n\ny");
        Assertions.assertThat(code("x\n\r\r\ry")).isEqualTo("x\n\ny");
    }

    @Test void testCollapseNewlines() {
        Assertions.assertThat(code("x\ny")).isEqualTo("x\ny");
        Assertions.assertThat(code("x\n\n\n\ny")).isEqualTo("x\ny");
    }

    @Test void testCollapseSpaces() {
        Assertions.assertThat(code("x y")).isEqualTo("x y");
        Assertions.assertThat(code("x    y")).isEqualTo("x y");
    }


    @Test void testIgnoreLeadingWhitespace() {
        Assertions.assertThat(code(" {}")).isEqualTo("{}");
        Assertions.assertThat(code("\n{}")).isEqualTo("{}");
        Assertions.assertThat(code("\r{}")).isEqualTo("{}");
        Assertions.assertThat(code("\f{}")).isEqualTo("{}");
        Assertions.assertThat(code("\f \n\r{}")).isEqualTo("{}");
    }

    @Test void testIgnoreTrailingWhitespace() {
        Assertions.assertThat(code("{} ")).isEqualTo("{}");
        Assertions.assertThat(code("{}\n")).isEqualTo("{}");
        Assertions.assertThat(code("{}\r")).isEqualTo("{}");
        Assertions.assertThat(code("{}\f")).isEqualTo("{}");
        Assertions.assertThat(code("{} \f\n\r")).isEqualTo("{}");
    }


    @Test void testIgnoreLineLeadingWhitespace() {
        Assertions.assertThat(code("x\n  x")).isEqualTo("x\nx");
    }

    @Test void testIgnoreLineTrailingWhitespace() {
        Assertions.assertThat(code("x  \nx")).isEqualTo("x\nx");
    }


    @Test void tesExpandFolds() {
        Assertions.assertThat(code("x\rx\n\rx")).isEqualTo("x x\n\nx");
    }

    @Test void testStripWhitespaceInsidePairs() {
        Assertions.assertThat(code("( x )")).isEqualTo("(x)");
        Assertions.assertThat(code("[ x ]")).isEqualTo("[x]");
        Assertions.assertThat(code("{ x }")).isEqualTo("{ x }");
    }


    @Test void testIndentBraceBlocks() {
        Assertions.assertThat(code("{\nx\n}\ny")).isEqualTo("{\n    x\n}\ny");
        Assertions.assertThat(code("{\f{ x }\f}")).isEqualTo("{\n\n    { x }\n\n}");
    }

    @Test void testInlineBraceBlocks() {
        Assertions.assertThat(code("{ {\nx\n} }\ny")).isEqualTo("{ {\n    x\n} }\ny");
    }

}
