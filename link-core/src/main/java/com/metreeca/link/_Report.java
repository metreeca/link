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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Collection;
import java.util.Map;

import static com.metreeca.link.Frame.asIRI;
import static com.metreeca.link.Frame.reverse;
import static com.metreeca.link.Trace.trace;
import static com.metreeca.link._Focus.focus;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.stream.Collectors.*;

public final class _Report {


    public static Map<Value, Trace> report(final Collection<Statement> model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return focus(SHACL.VALIDATION_REPORT, model)

                .shift(reverse(RDF.TYPE), SHACL.RESULT)

                .split().filter(result -> result
                        .shift(SHACL.RESULT_SEVERITY)
                        .values(asIRI())
                        .anyMatch(SHACL.VIOLATION::equals)
                )

                .collect(groupingBy(
                        _Report::node,
                        mapping(_Report::property, collectingAndThen(toList(), Trace::trace))
                ));
    }

    private static Value node(final _Focus result) {
        return result.shift(SHACL.FOCUS_NODE).value().orElseThrow(() ->
                new IllegalArgumentException("sh:ValidationResult without sh:focusNode")
        );
    }

    private static Trace property(final _Focus result) {
        return trace(path(result), trace(constraint(result)));
    }

    private static IRI path(final _Focus result) { // !!! reverse
        return result.shift(SHACL.RESULT_PATH).value(asIRI()).orElseThrow(() ->
                new IllegalArgumentException("sh:ValidationResult without sh:resultPath")
        );
    }

    private static String constraint(final _Focus result) {

        // !!! sh:value
        // !!! sh:message
        // !!! sh:details

        final IRI component=component(result);
        final IRI constraint=CONSTRAINTS.getOrDefault(component, component);

        final String zzz=result.shift(SHACL.SOURCE_SHAPE, constraint).value() // !!! multi-valued constraints
                .map(Value::stringValue)
                .orElse("");

        return format("%s(%s)", constraint.getLocalName(), zzz);
    }

    private static IRI component(final _Focus result) {
        return result.shift(SHACL.SOURCE_CONSTRAINT_COMPONENT).value(asIRI()).orElseThrow(() ->
                new IllegalArgumentException("sh:ValidationResult without sh:sourceConstraintComponent")
        );
    }


    private static final Map<IRI, IRI> CONSTRAINTS=Map.ofEntries(

            entry(SHACL.CLASS_CONSTRAINT_COMPONENT, SHACL.CLASS),
            entry(SHACL.DATATYPE_CONSTRAINT_COMPONENT, SHACL.DATATYPE),
            entry(SHACL.NODE_KIND_CONSTRAINT_COMPONENT, SHACL.NODE_KIND),

            entry(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT, SHACL.MIN_COUNT),
            entry(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT, SHACL.MAX_COUNT),

            entry(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT, SHACL.MIN_EXCLUSIVE),
            entry(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT, SHACL.MIN_INCLUSIVE),
            entry(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT, SHACL.MAX_EXCLUSIVE),
            entry(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT, SHACL.MAX_EXCLUSIVE),

            entry(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT, SHACL.MIN_LENGTH),
            entry(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT, SHACL.MAX_LENGTH),
            entry(SHACL.PATTERN_CONSTRAINT_COMPONENT, SHACL.PATTERN),
            entry(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT, SHACL.LANGUAGE_IN),
            entry(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT, SHACL.UNIQUE_LANG),

            entry(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT, SHACL.HAS_VALUE),
            entry(SHACL.IN_CONSTRAINT_COMPONENT, SHACL.IN)

    );

}
