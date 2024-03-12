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
import com.metreeca.link.Query;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.json._Parser.*;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

final class _Query {

    private static final String NULL="null";

    private static final Pattern PAIR_PATTERN=compile("&?(?<label>[^=&]*)(?:=(?<value>[^&]*))?");


    static Frame decode(final URI base, final String string, final Shape shape) {

        final List<Entry<IRI, Shape>> collections=shape.predicates().entrySet().stream()

                .map(e -> entry(e.getKey(), e.getValue().getValue().get()))

                .filter(e -> e.getValue().maxCount().orElse(Integer.MAX_VALUE) > 1)
                .filter(e -> !e.getValue().predicates().isEmpty())

                .collect(toList());

        if ( collections.size() == 1 ) {

            final Entry<IRI, Shape> collection=collections.iterator().next();

            final IRI predicate=collection.getKey();
            final Query query=_query(base, string, collection.getValue());

            return frame(field(predicate, query));

        } else if ( collections.isEmpty() ) {

            throw new IllegalArgumentException("no collection property found");

        } else {

            throw new IllegalArgumentException("multiple collection properties found");

        }
    }

    static Query _query(final URI base, final String query, final Shape shape) {

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

                final Expression expression=expression(label.substring(2), shape);

                queries.add(filter(expression, lte(value(value, expression.apply(shape), base))));

            } else if ( label.startsWith(">=") || label.startsWith(">>") ) {

                final Expression expression=expression(label.substring(2), shape);

                queries.add(filter(expression, gte(value(value, expression.apply(shape), base))));

            } else if ( label.startsWith("<") ) {

                final Expression expression=expression(label.substring(1), shape);

                queries.add(filter(expression, lt(value(value, expression.apply(shape), base))));

            } else if ( label.startsWith(">") ) {

                final Expression expression=expression(label.substring(1), shape);

                queries.add(filter(expression, gt(value(value, expression.apply(shape), base))));

            } else if ( label.startsWith("~") ) {

                final Expression expression=expression(label.substring(1), shape);

                queries.add(filter(expression, like(value)));

            } else if ( label.startsWith("^") ) {

                final Expression expression=expression(label.substring(1), shape);
                final int priority=priority(value);

                queries.add(order(expression, priority));

            } else if ( label.equals("@") ) {

                queries.add(offset(parseInt(value)));

            } else if ( label.equals("#") ) {

                queries.add(offset(parseInt(value)));

            } else {

                final Expression expression=expression(label, shape);

                options.compute(expression, (key, values) -> {

                    final Set<Value> set=values == null ? new LinkedHashSet<>() : values;

                    if ( !value.isBlank() ) {
                        set.add(value.equals(NULL) ? NIL : value(value, expression.apply(shape), base));
                    }

                    return set;
                });

            }

        }

        options.forEach((expression, values) -> queries.add(filter(expression, any(values))));

        return query(frame(), queries);
    }

}
