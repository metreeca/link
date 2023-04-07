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

import com.metreeca.rest.*;
import com.metreeca.rest.Query.Direction;
import com.metreeca.rest.Stash.Expression;

import org.eclipse.rdf4j.model.Resource;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.metreeca.rest.Query.Constraint;
import static com.metreeca.rest.Query.Direction.increasing;
import static com.metreeca.rest.Query.pattern;
import static com.metreeca.rest.Stash.Transform.count;
import static com.metreeca.rest.rdf4j.Coder.*;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.*;

final class SPARQLMembers extends com.metreeca.rest.rdf4j.SPARQL {

    private static final int DefaultLimit=100;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    String members(
            final Resource container,
            final Optional<String> predicate,
            final Shape shape,
            final Query<?> query
    ) {

        final Object template=query.template();

        final boolean plain=!(template instanceof Table);


        // filtering expressions

        final Set<Expression> filters=query.filters().keySet();

        // projected variable names to/from projected expressions

        final Map<String, Expression> alias2projected=plain ? Map.of() : ((Table<?>)template)
                .columns().entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> entry.getValue().expression()));

        final Map<Expression, String> projected2alias=alias2projected.entrySet().stream()
                .collect(toMap(entry -> Expression.expression(List.of(entry.getKey()), List.of()), Entry::getKey));

        // projected expressions to/from expressions referring to aliased variable names // !!! review

        final Map<Expression, Expression> expression2projected=alias2projected.entrySet().stream()
                .map(entry -> entry(entry.getValue(), Expression.expression(List.of(entry.getKey()), List.of())))
                .filter(not(entry -> entry.getKey().equals(entry.getValue()))) // ignore non-aliased expressions
                .collect(toMap(Entry::getKey, Entry::getValue));

        final Map<Expression, Expression> projected2expression=alias2projected.entrySet().stream()
                .map(entry -> entry(Expression.expression(List.of(entry.getKey()), List.of()), entry.getValue()))
                .filter(not(entry -> entry.getKey().equals(entry.getValue()))) // ignore non-aliased expressions
                .collect(toMap(Entry::getKey, Entry::getValue));

        // expression paths to required status

        final Map<List<String>, Boolean> paths=Stream

                .of(
                        alias2projected.values(),
                        query.filters().keySet(),
                        query.order().keySet()
                )

                .flatMap(Collection::stream)

                .filter(not(expression2projected::containsValue)) // not referring to a projected variable names
                .filter(not(expression -> expression.path().isEmpty())) // not referring to the root value

                .collect(groupingBy(Expression::path, reducing( // required
                        false,
                        e -> !e.aggregate() && (filters.contains(e) || filters.contains(expression2projected.get(e))),
                        (x, y) -> x || y
                )));


        final Coder member=var(id());

        return query(items(

                select(plain ? member : items(alias2projected.entrySet().stream() // !!! refactor
                        .map(projection -> {

                            final String alias=projection.getKey();
                            final Expression expression=projection.getValue();

                            return expression.aggregate() ? as(alias, expression(expression)) : var(alias);

                        })
                        .collect(toList())
                )),

                space(where(space(

                        // collection membership

                        predicate
                                .map(iri -> edge(resource(container), iri, member))
                                .orElseGet(Coder::nothing),

                        // member type constraint

                        shape.type()
                                .map(type -> space(edge(member, text("a"), iri(type))))
                                .orElseGet(Coder::nothing),

                        // raw required expression values

                        space(items(paths.entrySet().stream()
                                .filter(Entry::getValue)
                                .map(Entry::getKey)
                                .map(path -> line(edge(member, path(shape, path), var(id(path)))))
                                .collect(toList())
                        )),

                        // raw optional expression values

                        space(items(paths.entrySet().stream()
                                .filter(not(Entry::getValue))
                                .map(Entry::getKey)
                                .map(path -> line(optional(edge(member, path(shape, path), var(id(path))))))
                                .collect(toList())
                        )),

                        // non-aggregate computed values

                        space(items(alias2projected.entrySet().stream()
                                .filter(not(entry -> entry.getValue().aggregate()))
                                .map(entry -> line(bind(entry.getKey(), expression(entry.getValue()))))
                                .collect(toList())
                        )),

                        // non-aggregate filters

                        space(filters(query.filters().entrySet().stream()
                                .filter(not(entry -> projected2expression.getOrDefault(entry.getKey(), entry.getKey()).aggregate())) // !!! review
                                .flatMap(entry -> constraint(result(entry.getKey()), entry.getValue()))
                                .collect(toList()))
                        )

                ))),

                // aggregate grouping // !!! refactor

                space(plain && filters.stream().anyMatch(Expression::aggregate) ? group(member)
                        : !plain && alias2projected.values().stream().anyMatch(not(Expression::aggregate)) ?
                        group(items(
                                alias2projected.entrySet().stream()
                                        .filter(not(entry -> entry.getValue().aggregate()))
                                        .map(entry -> var(entry.getKey()))
                                        .collect(toList())
                        ))
                        : nothing()
                ),

                // aggregate filters // !!! refactor

                space(havings(query.filters().entrySet().stream()
                        .filter(entry -> projected2expression.getOrDefault(entry.getKey(), entry.getKey()).aggregate()) // !!! review
                        .flatMap(entry -> {

                            final String alias=projected2alias.get(entry.getKey());

                            return constraint(alias != null ? var(alias) : result(entry.getKey()), entry.getValue());
                        })

                        .collect(toList()))
                ),

                // sorting // !!! refactor

                space(plain

                        ?
                        order(items(

                                // explicit criteria

                                items(query.order().entrySet().stream()
                                        .map(entry -> {

                                            final Direction direction=entry.getValue();
                                            final Expression expression=entry.getKey();

                                            final Coder result=result(expression);

                                            return direction == increasing ? asc(result) : desc(result);

                                        })
                                        .collect(toList())
                                ),

                                // default criteria

                                asc(member)

                        ))

                        // all aggregates >> single record >> no order

                        : alias2projected.values().stream().anyMatch(not(Expression::aggregate))

                        ?
                        order(items(

                                // explicit criteria

                                items(query.order().entrySet().stream()
                                        .map(entry -> {

                                            final Direction direction=entry.getValue();
                                            final Expression expression=entry.getKey();

                                            final String alias=projected2alias.get(expression);

                                            final Coder result=alias != null ? var(alias) :
                                                    result(expression);

                                            return direction == increasing ? asc(result) : desc(result);

                                        })
                                        .collect(toList())
                                ),

                                // default criteria

                                // !!! exclude projected values already ordered

                                items(alias2projected.keySet().stream()
                                        .map(this::var)
                                        .collect(toList())
                                )

                        ))

                        : nothing()
                ),

                // range

                space(
                        line(offset(query.offset())),
                        line(limit(Optional.of(query.limit()).filter(v -> v > 0).orElse(DefaultLimit)))
                )

        ));
    }


    private Coder filters(final Collection<Coder> coders) {
        return coders.isEmpty() ? nothing() : filter(indent(list("\n&& ", coders)));
    }

    private Coder havings(final Collection<Coder> coders) {
        return coders.isEmpty() ? nothing() : having(indent(list("\n&& ", coders)));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder result(final Expression expression) {
        return var(id(expression.computed() ? expression : expression.path()));
    }

    private Coder expression(final Expression expression) {
        return transform(
                expression.transforms(),
                expression.path().isEmpty() ? text("*") : var(id(expression.path()))
        );
    }

    private Coder transform(final List<Stash.Transform> transforms, final Coder value) {
        if ( transforms.isEmpty() ) { return value; } else {

            final Stash.Transform head=transforms.get(0);
            final List<Stash.Transform> tail=transforms.subList(1, transforms.size());

            return head == count
                    ? count(true, transform(tail, value))
                    : function(head.name(), transform(tail, value));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder path(final Shape shape, final Collection<String> path) {
        return list("/", path.stream()
                .map(step -> shape.property(step).orElseThrow(() ->
                        new IllegalArgumentException(format(
                                "unknown shape property <%s>", step
                        ))
                ))
                .map(this::iri)
                .collect(toList())
        );
    }

    private Stream<Coder> constraint(final Coder value,
            final Constraint constraint) {
        return Stream.

                of(

                        constraint.lt().map(limit -> (_lt(value, limit))).stream(),
                        constraint.gt().map(limit -> (_gt(value, limit))).stream(),

                        constraint.lte().map(limit -> (_lte(value, limit))).stream(),
                        constraint.gte().map(limit -> (_gte(value, limit))).stream(),

                        constraint.like().stream().map(keywords -> (like(value, keywords))),
                        constraint.any().stream().map(values -> any(value, values))

                )

                .flatMap(identity());
    }


    private Coder _lt(final Coder value, final Object limit) {
        return lt(value, encode(limit));
    }

    private Coder _gt(final Coder value, final Object limit) {
        return gt(value, encode(limit));
    }


    private Coder _lte(final Coder value, final Object limit) {
        return lte(value, encode(limit));
    }

    private Coder _gte(final Coder value, final Object limit) {
        return gte(value, encode(limit));
    }


    private Coder like(final Coder value, final String keywords) {
        return regex(str(value), quoted(pattern(keywords, true)));
    }

    private Coder any(final Coder value, final Collection<Object> any) { // !!! empty list  / null values
        return in(value, any.stream()
                .map(this::encode)
                .collect(toList())
        );
    }

}