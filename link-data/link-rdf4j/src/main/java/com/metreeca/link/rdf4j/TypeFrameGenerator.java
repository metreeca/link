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

import com.metreeca.link.*;
import com.metreeca.link.Query.Criterion;
import com.metreeca.link.Stash.Expression;
import com.metreeca.link.Stash.Transform;
import com.metreeca.link.Table.Column;

import org.eclipse.rdf4j.model.Resource;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.link.Query.Constraint;
import static com.metreeca.link.Query.Criterion.increasing;
import static com.metreeca.link.Query.pattern;
import static com.metreeca.link.Stash.Transform.count;
import static com.metreeca.link.rdf4j.Coder.*;
import static com.metreeca.link.rdf4j.SPARQL.and;
import static com.metreeca.link.rdf4j.SPARQL.as;
import static com.metreeca.link.rdf4j.SPARQL.asc;
import static com.metreeca.link.rdf4j.SPARQL.bind;
import static com.metreeca.link.rdf4j.SPARQL.bound;
import static com.metreeca.link.rdf4j.SPARQL.count;
import static com.metreeca.link.rdf4j.SPARQL.desc;
import static com.metreeca.link.rdf4j.SPARQL.edge;
import static com.metreeca.link.rdf4j.SPARQL.eq;
import static com.metreeca.link.rdf4j.SPARQL.filter;
import static com.metreeca.link.rdf4j.SPARQL.function;
import static com.metreeca.link.rdf4j.SPARQL.having;
import static com.metreeca.link.rdf4j.SPARQL.in;
import static com.metreeca.link.rdf4j.SPARQL.iri;
import static com.metreeca.link.rdf4j.SPARQL.limit;
import static com.metreeca.link.rdf4j.SPARQL.not;
import static com.metreeca.link.rdf4j.SPARQL.offset;
import static com.metreeca.link.rdf4j.SPARQL.optional;
import static com.metreeca.link.rdf4j.SPARQL.or;
import static com.metreeca.link.rdf4j.SPARQL.orderBy;
import static com.metreeca.link.rdf4j.SPARQL.regex;
import static com.metreeca.link.rdf4j.SPARQL.select;
import static com.metreeca.link.rdf4j.SPARQL.star;
import static com.metreeca.link.rdf4j.SPARQL.str;
import static com.metreeca.link.rdf4j.SPARQL.value;
import static com.metreeca.link.rdf4j.SPARQL.var;
import static com.metreeca.link.rdf4j.SPARQL.where;

import static java.lang.String.format;
import static java.util.function.Predicate.not;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class TypeFrameGenerator {

    private static final int DefaultLimit=100;

    private static final Expression Root=Stash.expression("");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Object, String> scope=new HashMap<>();


    String id() {
        return id(List.of());
    }

    String id(final String alias, final Column column) {
        return id(alias, column.expression());
    }


    private String id(final String alias, final Expression expression) {
        return expression.aggregate() ? id(alias) : id(expression);
    }

    private String id(final Expression expression) {
        return id(expression.computed() ? expression : expression.path());
    }


    private String id(final Object object) {
        return scope.computeIfAbsent(object, o -> String.valueOf(scope.size()));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder members(
            final Resource container,
            final Optional<String> predicate,
            final Shape shape,
            final Query<?> query
    ) {

        final URI base=URI.create(container.isIRI() ? container.stringValue() : Frame.DefaultBase);
        final Object model=query.model();

        final boolean plain=!(model instanceof Table);

        final Map<String, Expression> projection=plain ? Map.of() : projection((Table<?>)model);

        final Map<Expression, Constraint> filter=resolve(projection, query.filter());
        final Map<Expression, Set<Object>> focus=resolve(projection, query.focus());
        final Map<Expression, Criterion> order=resolve(projection, query.order());

        final boolean grouping=(

                projection.isEmpty()
                        || projection.values().stream().anyMatch(not(Expression::aggregate))

        ) && (

                projection.values().stream().anyMatch(Expression::aggregate)
                        || filter.keySet().stream().anyMatch(Expression::aggregate)
                        || order.keySet().stream().anyMatch(Expression::aggregate)
                        || focus.keySet().stream().anyMatch(Expression::aggregate)

        );

        final Coder member=var(id());

        return items(

                select(plain, plain ? member : projection(projection)),

                space(where(space(

                        // collection membership

                        predicate
                                .map(iri -> edge(value(container), iri, member))
                                .orElseGet(Coder::nothing),

                        // member type constraint

                        shape.types().findFirst() // !!! only most-specific?
                                .map(type -> space(edge(member, text("a"), iri(type))))
                                .orElseGet(Coder::nothing),

                        // raw expression values

                        space(items(Stream

                                .of(
                                        projection.values(),
                                        filter.keySet(),
                                        focus.keySet(),
                                        order.keySet()
                                )

                                .flatMap(Collection::stream)
                                .map(Expression::path)
                                .distinct()

                                .filter(not(List::isEmpty)) // not referring to the root value

                                .map(path -> line(optional(edge(member,

                                        shape.properties(path).orElseThrow(() ->
                                                new IllegalArgumentException(format(
                                                        "unknown shape path <%s>", path
                                                ))
                                        ),

                                        var(id(path))

                                ))))

                                .collect(toList())
                        )),

                        // non-aggregate computed values

                        space(items(Stream

                                .of(
                                        projection.values(),
                                        filter.keySet(),
                                        focus.keySet(),
                                        order.keySet()
                                )

                                .flatMap(Collection::stream)

                                .filter(not(Expression::aggregate))
                                .filter(Expression::computed)

                                .map(expression -> line(bind(expression(expression), id(expression))))
                                .collect(toList())
                        )),

                        // non-aggregate filters

                        space(filter(filter.entrySet().stream()
                                .filter(not(entry -> entry.getKey().aggregate()))
                                .flatMap(entry -> constraint(var(id(entry.getKey())), entry.getValue(), base))
                                .collect(toList()))
                        )

                ))),

                // grouping

                space(grouping ?

                        SPARQL.groupBy(plain ? member : items(projection.values().stream()
                                .filter(not(Expression::aggregate))
                                .map(expression -> var(id(expression)))
                                .collect(toList())
                        ))

                        : nothing()

                ),

                // aggregate filters

                space(having(filter.entrySet().stream()
                        .filter(entry -> entry.getKey().aggregate())
                        .flatMap(entry -> constraint(expression(entry.getKey()), entry.getValue(), base))
                        .collect(toList()))
                ),

                // sorting

                space(plain ?

                        orderBy(items(

                                focus(base, focus), // focus values
                                order(order), // explicit criteria

                                order.containsKey(Root) ? nothing() : asc(member) // default criteria

                        ))

                        : projection.values().stream().anyMatch(not(Expression::aggregate)) ?

                        orderBy(items(

                                focus(base, focus), // focus values
                                order(order), // explicit criteria

                                items(projection.entrySet().stream() // default criteria
                                        .filter(not(entry -> order.containsKey(entry.getValue())))
                                        .map(entry -> asc(var(id(entry.getKey(), entry.getValue()))))
                                        .collect(toList())
                                )

                        ))

                        // all aggregates >> single record >> no order

                        : nothing()

                ),

                // range

                space(
                        line(offset(query.offset())),
                        line(limit(Optional.of(query.limit()).filter(v -> v > 0).orElse(DefaultLimit)))
                )

        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Map<String, Expression> projection(final Table<?> model) {

        final Map<String, Expression> projection=new LinkedHashMap<>();

        model.columns().forEach((alias, column) -> projection.put(alias, column.expression()));

        return projection;
    }


    private static <V> Map<Expression, V> resolve(final Map<String, Expression> projection, final Map<Expression, V> map) {

        final Map<Expression, V> expanded=new LinkedHashMap<>();

        map.forEach((expression, value) -> expanded.put(resolve(projection, expression), value));

        return expanded;
    }

    private static Expression resolve(final Map<String, Expression> projection, final Expression expression) {
        return Optional.of(expression.path())
                .filter(path -> path.size() == 1)
                .map(path -> path.get(0))
                .map(projection::get)
                .map(projected -> Stash.expression(
                        projected.path(),
                        Stream.of(expression.transforms(), projected.transforms())
                                .flatMap(Collection::stream)
                                .collect(toList())
                ))
                .orElse(expression);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder projection(final Map<String, Expression> projection) {
        return projection.isEmpty() ? star() : items(projection.entrySet().stream()
                .map(entry -> {

                    final String alias=entry.getKey();
                    final Expression expression=entry.getValue();

                    return expression.aggregate() ? as(expression(expression), id(alias)) : var(id(expression));

                })
                .collect(toList())
        );
    }


    private Coder focus(final URI base, final Map<Expression, Set<Object>> focus) {
        return items(focus.entrySet().stream()
                .map(entry -> {

                    final Expression expression=entry.getKey();
                    final Set<Object> values=entry.getValue();

                    return focus(values, base, expression.aggregate()
                            ? expression(expression)
                            : var(id(expression))
                    );

                })
                .collect(toList())
        );
    }

    private Coder order(final Map<Expression, Criterion> order) {
        return items(order.entrySet().stream()
                .map(entry -> {

                    final Expression expression=entry.getKey();
                    final Criterion criterion=entry.getValue();

                    return order(criterion, expression.aggregate()
                            ? expression(expression)
                            : var(id(expression))
                    );

                })
                .collect(toList())
        );
    }


    private Coder focus(final Collection<Object> values, final URI base, final Coder value) {

        final boolean nulls=values.stream().anyMatch(Objects::isNull);
        final boolean nonNulls=values.stream().anyMatch(Objects::nonNull);

        final Coder nb=not(bound(value));

        final Coder in=in(value, values.stream()
                .filter(Objects::nonNull)
                .map(v -> value(v, base))
                .collect(toList())
        );

        return desc(nulls && nonNulls ? or(nb, in)
                : nonNulls ? and(bound(value), in)
                : nb
        );
    }

    private Coder order(final Criterion criterion, final Coder value) {
        return criterion == increasing ? asc(value) : desc(value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder expression(final Expression expression) {
        return transform(
                expression.transforms(),
                expression.path().isEmpty() ? star() : var(id(expression.path()))
        );
    }

    private Coder transform(final List<Transform> transforms, final Coder value) {
        if ( transforms.isEmpty() ) { return value; } else {

            final Transform head=transforms.get(0);
            final List<Transform> tail=transforms.subList(1, transforms.size());

            return head == count
                    ? count(true, transform(tail, value))
                    : function(head.name(), transform(tail, value));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Stream<Coder> constraint(final Coder value, final Constraint constraint, final URI base) {
        return Stream.

                of(

                        constraint.lt().map(limit -> (lt(value, limit, base))).stream(),
                        constraint.gt().map(limit -> (gt(value, limit, base))).stream(),

                        constraint.lte().map(limit -> (lte(value, limit, base))).stream(),
                        constraint.gte().map(limit -> (gte(value, limit, base))).stream(),

                        constraint.like().stream().map(keywords -> (like(value, keywords))),
                        constraint.any().stream().map(values -> any(value, values, base))

                )

                .flatMap(identity());
    }


    private Coder lt(final Coder value, final Object limit, final URI base) {
        return SPARQL.lt(value, value(limit, base));
    }

    private Coder gt(final Coder value, final Object limit, final URI base) {
        return SPARQL.gt(value, value(limit, base));
    }


    private Coder lte(final Coder value, final Object limit, final URI base) {
        return SPARQL.lte(value, value(limit, base));
    }

    private Coder gte(final Coder value, final Object limit, final URI base) {
        return SPARQL.gte(value, value(limit, base));
    }


    private Coder like(final Coder value, final String keywords) {
        return regex(str(value), quoted(pattern(keywords, true)));
    }

    private Coder any(final Coder value, final Collection<Object> any, final URI base) {

        final boolean existential=any.isEmpty();
        final boolean positive=any.stream().noneMatch(Objects::isNull);

        final Set<Object> options=any.stream().filter(Objects::nonNull).collect(toSet());

        final Coder blank=not(bound(value));
        final Coder values=options.size() == 1
                ? eq(value, value(options.iterator().next(), base))
                : in(value, options.stream().map(v -> value(v, base)).collect(toList()));

        return existential ? bound(value)
                : positive ? values
                : options.isEmpty() ? blank
                : parens(or(blank, values));
    }

}
