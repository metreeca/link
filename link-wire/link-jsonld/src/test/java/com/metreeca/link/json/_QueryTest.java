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

package com.metreeca.link.json;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Shape.*;
import static com.metreeca.link.json._Query._query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class _QueryTest {

    private static final URI base=URI.create("https://example.org/base/");

    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");
    private static final IRI z=iri("test:z");


    @Test void testDecodeEmptyQueries() {
        assertThat(_query(base, "", property(x))).satisfies(query -> {
            assertThat(query.model().fields()).isEmpty();
            assertThat(query.filter()).isEmpty();
            assertThat(query.order()).isEmpty();
            assertThat(query.focus()).isEmpty();
        });
    }

    @Test void testDecodeEmptyPaths() {
        assertThat(_query(base, "=value", shape()).filter().get(expression()).any())
                .contains(Set.of(literal("value")));
    }

    @Test void testDecodeSingletonPaths() {
        assertThat(_query(base, "x=value", property(x)).filter().get(expression(x)).any())
                .contains(Set.of(literal("value")));
    }

    @Test void testDecodePaths() {
        assertThat(_query(base, "x.y.z=value",
                property(x, property(y, property(z)))
        ).filter().get(expression(x, y, z)).any())
                .contains(Set.of(literal("value")));
    }


    @Test void testReportUnknownPropertyLabels() {
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "w", shape()));
    }

    @Test void testReportMalformedPropertyLabels() {
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "±w", shape()));
    }


    @Test void testDecodeLtConstraints() {
        assertThat(_query(base, "<x=value", property(x)).filter().get(expression(x)).lt())
                .contains(literal("value"));
    }

    @Test void testDecodeGtConstraints() {
        assertThat(_query(base, ">x=value", property(x)).filter().get(expression(x)).gt())
                .contains(literal("value"));
    }

    @Test void testDecodeLteConstraints() {
        assertThat(_query(base, "<%3Dx=value", property(x)).filter().get(expression(x)).lte())
                .contains(literal("value"));
    }

    @Test void testDecodeGteConstraints() {
        assertThat(_query(base, ">%3Dx=value", property(x)).filter().get(expression(x)).gte())
                .contains(literal("value"));
    }

    @Test void testDecodeAlternateLteConstraints() {
        assertThat(_query(base, "<<x=value", property(x)).filter().get(expression(x)).lte())
                .contains(literal("value"));
    }

    @Test void testDecodeAlternateGteConstraints() {
        assertThat(_query(base, ">>x=value", property(x)).filter().get(expression(x)).gte())
                .contains(literal("value"));
    }


    @Test void testDecodeLikeConstraints() {
        assertThat(_query(base, "~x=value", property(x)).filter().get(expression(x)).like())
                .contains("value");
    }


    @Test void testDecodeSingletonAnyConstraints() {
        assertThat(_query(base, "x=value", property(x)).filter().get(expression(x)).any())
                .contains(Set.of(literal("value")));
    }

    @Test void testDecodeMultipleAnyConstraints() {
        assertThat(_query(base, "x=1&x=2", property(x)).filter().get(expression(x)).any())
                .contains(Set.of(literal("1"), literal("2")));
    }

    @Test void testDecodeNonExistentialAnyConstraints() {
        assertThat(_query(base, "x=null", property(x)).filter().get(expression(x)).any())
                .contains(Set.of(NIL));
    }

    @Test void testDecodeExistentialAnyConstraints() {
        assertThat(_query(base, "x", property(x)).filter().get(expression(x)).any())
                .contains(Set.of());
    }


    @Test void testDecodeAscendingOrder() {
        assertThat(_query(base, "^x=1", property(x)).order().get(expression(x)))
                .isEqualTo(1);
    }

    @Test void testDecodeDescendingOrder() {
        assertThat(_query(base, "^x=-123", property(x)).order().get(expression(x)))
                .isEqualTo(-123);
    }

    @Test void testDecodeAlternateIncreasingOrder() {
        assertThat(_query(base, "^x=increasing", property(x)).order().get(expression(x)))
                .isEqualTo(1);
    }

    @Test void testDecodeAlternateDecreasingOrder() {
        assertThat(_query(base, "^x=decreasing", property(x)).order().get(expression(x)))
                .isEqualTo(-1);
    }

    @Test void testReportMalformedOrder() {
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "^x=1.23", property(x)));
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "^x=value", property(x)));
    }


    @Test void testDecodeMergeMultipleConstraints() {
        assertThat(_query(base, ">>x=lower&<<x=upper", property(x)).filter()).satisfies(filter -> {
            assertThat(filter.get(expression(x)).gte()).contains(literal("lower"));
            assertThat(filter.get(expression(x)).lte()).contains(literal("upper"));
        });
    }


    @Test void testDecodeOffset() {
        assertThat(_query(base, "@=123", property(x)).offset())
                .isEqualTo(123);

    }

    @Test void testReportMalformedOffset() {
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "@=", property(x)));
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "@=value", property(x)));
    }


    @Test void testDecodeLimit() {
        assertThat(_query(base, "#=123", property(x)).offset())
                .isEqualTo(123);

    }

    @Test void testReportMalformedLimit() {
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "#=", property(x)));
        assertThatIllegalArgumentException().isThrownBy(() -> _query(base, "#=value", property(x)));
    }


    @Test void testAssignKnownDatatype() {
        assertThat(_query(base, "<x=1", property(x, datatype(XSD.INTEGER))).filter().get(expression(x)).lt())
                .contains(literal(integer(1)));
    }

}