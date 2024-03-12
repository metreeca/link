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

import com.metreeca.link.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Probe.probe;
import static com.metreeca.link.Query.*;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;

public final class _Expression {

    private static final Pattern LABEL_PATTERN=compile("((?<unquoted>\\w+)|'(?<quoted>([^'\\x00-\\x19]|'')*)')");
    private static final Pattern PROBE_PATTERN=compile(LABEL_PATTERN+"=");
    private static final Pattern TRANSFORM_PATTERN=compile(LABEL_PATTERN+":");
    private static final Pattern FIELD_PATTERN=compile("\\.?"+LABEL_PATTERN);
    private static final Pattern ESCAPED_PATTERN=compile("''");

    private static final Pattern PAIR_PATTERN=compile("&?(?<label>[^=&]*)(?:=(?<value>[^=&]*))?");


    static Entry<IRI, Shape> _predicate(final String predicate, final Shape shape) {

        final Matcher matcher=PROBE_PATTERN.matcher(predicate);

        if ( matcher.lookingAt() ) {

            final String alias=label(matcher);
            final Expression expression=_expression(predicate.substring(matcher.end()), shape);

            return entry(probe(alias, expression), expression.apply(shape));

        } else {

            return shape.entry(predicate).orElseThrow(() ->
                    new IllegalArgumentException(format("unknown property label <%s>", predicate))
            );

        }
    }

    static Expression _expression(final String expression, final Shape shape) {

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

                throw new CodecException(format("unknown transform <%s>", label));

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
            throw new CodecException(format("malformed expression <%s>", expression));
        }

        return expression(pipe, path);
    }

    static Query _query(final String query, final Shape shape) {

        final Collection<Query> queries=new ArrayList<>();
        final Map<Expression, Set<Value>> options=new HashMap<>();

        for (
                final Matcher matcher=PAIR_PATTERN.matcher(query);
                matcher.lookingAt() && matcher.start() < query.length();
                matcher.region(matcher.end(), query.length())
        ) {

            final String label=URLDecoder.decode(matcher.group("label"), UTF_8);
            final String value=URLDecoder.decode(Optional.ofNullable(matcher.group("value")).orElse(""), UTF_8);

            if ( label.startsWith("<=") || label.startsWith("<<") ) {

                final Expression expression=_expression(label.substring(2), shape);

                queries.add(filter(expression, lte(value(value, expression.apply(shape)))));

            } else if ( label.startsWith(">=") || label.startsWith(">>") ) {

                final Expression expression=_expression(label.substring(2), shape);

                queries.add(filter(expression, gte(value(value, expression.apply(shape)))));

            } else if ( label.startsWith("<") ) {

                final Expression expression=_expression(label.substring(1), shape);

                queries.add(filter(expression, lt(value(value, expression.apply(shape)))));

            } else if ( label.startsWith(">") ) {

                final Expression expression=_expression(label.substring(1), shape);

                queries.add(filter(expression, gt(value(value, expression.apply(shape)))));

            } else if ( label.startsWith("~") ) {

                final Expression expression=_expression(label.substring(1), shape);

                queries.add(filter(expression, like(value)));

            } else if ( label.startsWith("^") ) {

                final Expression expression=_expression(label.substring(1), shape);
                final int priority=priority(value);

                queries.add(order(expression, priority));

            } else if ( label.equals("@") ) {

                queries.add(offset(parseInt(value)));

            } else if ( label.equals("#") ) {

                queries.add(offset(parseInt(value)));

            } else {

                final Expression expression=_expression(label, shape);

                options.compute(expression, (key, values) -> {

                    final Set<Value> set=values == null ? new LinkedHashSet<>() : values;

                    if ( !value.isBlank() ) {
                        set.add(value.equals("null") ? NIL : value(value, expression.apply(shape)));
                    }

                    return set;
                });

            }

        }

        options.forEach((expression, values) -> queries.add(filter(expression, any(values))));

        return query(frame(), queries);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String label(final Matcher matcher) {

        final String unquoted=matcher.group("unquoted");
        final String quoted=matcher.group("quoted");

        return unquoted != null ? unquoted : ESCAPED_PATTERN.matcher(quoted).replaceAll("'");
    }


    //// !!! Factor w/ JSONDecoder /////////////////////////////////////////////////////////////////////////////////////

    private static final Map<String, Integer> ORDER=Map.of(
            "", +1,
            "increasing", +1,
            "decreasing", -1
    );


    private static Value value(final String value, final Shape shape) {
        return shape.datatype()
                .map(iri -> iri.equals(RESOURCE) ? wrap(shape, resource(value))
                        : iri.equals(BNODE) ? wrap(shape, bnode(value))
                        : iri.equals(IRI) ? wrap(shape, iri(value))
                        : literal(value, iri)
                ).orElseGet(() -> literal(value));
    }


    private static Resource resource(final String resource) {
        return resource.startsWith("_:") ? bnode(resource.substring(2)) : iri(resource);
    }

    private static IRI iri(final String iri) {
        try {

            final URI uri=new URI(iri); // !!! resolve

            if ( !uri.isAbsolute() ) {
                throw new CodecException(format("relative iri <%s>", uri.toASCIIString()));
            }

            return Frame.iri(uri);

        } catch ( final URISyntaxException e ) {

            throw new CodecException(format("malformed iri <%s>", iri));

        }
    }

    private static Value wrap(final Shape shape, final Resource resource) {
        return shape.labels().isEmpty() ? resource : frame(field(ID, resource));
    }

    private static int priority(final String priority) {
        return Optional.of(priority)

                .map(s -> s.toLowerCase(ROOT))
                .map(ORDER::get)

                .orElseGet(() -> {

                    try {

                        return parseInt(priority);

                    } catch ( final NumberFormatException e ) {

                        throw new CodecException(format("malformed priority value <%s>", priority));

                    }

                });
    }

}
