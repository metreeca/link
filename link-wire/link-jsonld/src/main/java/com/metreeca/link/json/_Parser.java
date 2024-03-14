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

import com.metreeca.link.Expression;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Transform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Probe.probe;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;

final class _Parser {

    private static final Pattern LABEL_PATTERN=compile("((?<unquoted>\\w+)|'(?<quoted>([^'\\x00-\\x19]|'')*)')");
    private static final Pattern PROBE_PATTERN=compile(LABEL_PATTERN+"=");
    private static final Pattern TRANSFORM_PATTERN=compile(LABEL_PATTERN+":");
    private static final Pattern FIELD_PATTERN=compile("\\.?"+LABEL_PATTERN);
    private static final Pattern ESCAPED_PATTERN=compile("''");


    static Entry<IRI, Shape> predicate(final String predicate, final Shape shape) {

        final Matcher matcher=PROBE_PATTERN.matcher(predicate);

        if ( matcher.lookingAt() ) {

            final String label=label(matcher);
            final Expression expression=expression(predicate.substring(matcher.end()), shape);

            return entry(probe(label, expression), expression.apply(shape));

        } else {

            return shape.entry(predicate).orElseThrow(() ->
                    new IllegalArgumentException(LABEL_PATTERN.matcher(predicate).matches()
                            ? format("unknown property label <%s>", predicate)
                            : format("malformed property label <%s>", predicate)
                    )
            );

        }
    }

    static Expression expression(final String expression, final Shape shape) {

        final List<Transform> pipe=new ArrayList<>();
        final List<IRI> path=new ArrayList<>();

        int next=0;

        final int length=expression.length();

        for (
                final Matcher matcher=TRANSFORM_PATTERN.matcher(expression).region(next, length);
                matcher.lookingAt();
                matcher.region(next, length)
        ) {

            final String label=label(matcher);

            try {

                pipe.add(Transform.valueOf(label.toUpperCase(ROOT)));

            } catch ( final IllegalArgumentException ignored ) {

                throw new IllegalArgumentException(format("unknown transform <%s>", label));

            }

            next=matcher.end();
        }

        Shape s=shape;

        for (
                final Matcher matcher=FIELD_PATTERN.matcher(expression).region(next, length);
                matcher.lookingAt();
                matcher.region(next, length)
        ) {

            final String label=label(matcher);

            final Entry<IRI, Shape> entry=s.entry(label).orElseThrow(() ->
                    new IllegalArgumentException(format("unknown property label <%s>", label))
            );

            path.add(entry.getKey());

            s=entry.getValue();
            next=matcher.end();
        }

        if ( next < length ) {
            throw new IllegalArgumentException(format("malformed expression <%s>", expression));
        }

        return Expression.expression(pipe, path);
    }


    private static String label(final Matcher matcher) {

        final String unquoted=matcher.group("unquoted");
        final String quoted=matcher.group("quoted");

        return unquoted != null ? unquoted : ESCAPED_PATTERN.matcher(quoted).replaceAll("'");
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static Value value(final String value, final Shape shape, final URI base) {
        return shape.datatype()
                .map(iri -> iri.equals(RESOURCE) ?

                        value(value.startsWith("_:")
                                ? bnode(value.substring(2))
                                : iri(value, base), shape
                        )

                        : iri.equals(BNODE) ? value(bnode(value), shape)
                        : iri.equals(IRI) ? value(iri(value, base), shape)

                        : iri.equals(RDF.LANGSTRING) ? literal(value)

                        : literal(value, iri)

                ).orElseGet(() -> literal(value));
    }


    private static Value value(final Resource resource, final Shape shape) {
        return shape.labels().isEmpty() ? resource : frame(field(ID, resource));
    }

    private static IRI iri(final String iri, final URI base) {
        try {

            final URI uri=base.resolve(iri); // !!! resolve

            if ( !uri.isAbsolute() ) {
                throw new IllegalArgumentException(format("relative iri <%s>", uri.toASCIIString()));
            }

            return Frame.iri(uri);

        } catch ( final IllegalArgumentException e ) {

            throw new IllegalArgumentException(format("malformed iri <%s>: %s", iri, e.getMessage()));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Map<String, Integer> ORDER=Map.of(
            "", +1,
            "increasing", +1,
            "decreasing", -1
    );


    static int priority(final String priority) {
        return Optional.of(priority)

                .map(s -> s.toLowerCase(ROOT))
                .map(ORDER::get)

                .orElseGet(() -> {

                    try {

                        return parseInt(priority);

                    } catch ( final NumberFormatException e ) {

                        throw new IllegalArgumentException(format("malformed priority value <%s>", priority), e);

                    }

                });
    }


    private _Parser() { }

}
