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

import org.eclipse.rdf4j.model.Resource;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.link.Query.Constraint;
import static com.metreeca.link.Query.Criterion.increasing;
import static com.metreeca.link.Query.pattern;
import static com.metreeca.link.Stash.Transform.count;
import static com.metreeca.link.rdf4j.Coder.*;

import static java.lang.String.format;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.*;

final class SPARQLMembers extends SPARQL {

    private static final int DefaultLimit=100;

    private <T> Predicate<T> not(final Predicate<T> predicate) { // !!! review
        return Predicate.not(predicate);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    String members(
            final Resource container,
            final Optional<String> predicate,
            final Shape shape,
            final Query<?> query
    ) {

        final URI base=URI.create(container.isIRI() ? container.stringValue() : Frame.DefaultBase);
        final Object model=query.model();


        final boolean plain=!(model instanceof Table);

        final Map<List<String>, Expression> projection=plain ? Map.of() : ((Table<?>)model)
                .columns().entrySet().stream()
                .collect(toMap(entry -> List.of(entry.getKey()), entry -> entry.getValue().expression()));

        final Map<Expression, Constraint> filter=query.filter().entrySet().stream()
                .collect(toMap(entry -> expand(projection, entry.getKey()), Entry::getValue));

        final Map<Expression, Set<Object>> focus=query.focus().entrySet().stream()
                .collect(toMap(entry -> expand(projection, entry.getKey()), Entry::getValue));

        final Map<Expression, Criterion> order=query.order().entrySet().stream()
                .collect(toMap(entry -> expand(projection, entry.getKey()), Entry::getValue));


        final Coder member=var(id());

        return query(items(

                select(plain, plain ? member : projection(projection)),

                space(where(space(

                        // collection membership

                        predicate
                                .map(iri -> edge(resource(container), iri, member))
                                .orElseGet(Coder::nothing),

                        // member type constraint

                        shape.types().findFirst() // !!! only first?
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

                                .filter(not(List::isEmpty)) // not referring to the root value

                                .map(path -> line(optional(edge(member,

                                        path(shape.properties(path).orElseThrow(() ->
                                                new IllegalArgumentException(format(
                                                        "unknown shape path <%s>", path
                                                ))
                                        )),

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

                                .map(expression -> line(bind(id(expression), expression(expression))))
                                .collect(toList())
                        )),

                        // non-aggregate filters

                        space(filter(filter.entrySet().stream()
                                .filter(not(entry -> entry.getKey().aggregate()))
                                .flatMap(entry -> constraint(result(entry.getKey()), entry.getValue(), base))
                                .collect(toList()))
                        )

                ))),

                // aggregate grouping // !!! refactor

                space(plain && filter.keySet().stream().anyMatch(Expression::aggregate) ?

                        group(member)

                        : projection.values().stream().anyMatch(not(Expression::aggregate)) ?

                        group(items(projection.values().stream()
                                .filter(not(Expression::aggregate))
                                .map(this::result)
                                .collect(toList())
                        ))

                        : nothing()
                ),

                // aggregate filters // !!! refactor

                space(having(filter.entrySet().stream()
                        .filter(entry -> entry.getKey().aggregate())
                        .flatMap(entry -> {

                            final String alias=null; // !!!

                            return constraint(alias != null ? var(alias) : result(entry.getKey()), entry.getValue(), base);

                        })

                        .collect(toList()))
                ),

                // sorting // !!! refactor

                space(plain ?

                        order(items(

                                // focus values

                                items(focus.entrySet().stream()
                                        .map(entry -> {

                                            final Expression expression=entry.getKey();
                                            final Set<Object> values=entry.getValue();

                                            return focus(null, expression, values, base);

                                        })
                                        .collect(toList())
                                ),

                                // explicit criteria

                                items(order.entrySet().stream()
                                        .map(entry -> {

                                            final Expression expression=entry.getKey();
                                            final Criterion criterion=entry.getValue();

                                            return order(null, expression, criterion);

                                        })
                                        .collect(toList())
                                ),

                                // default criteria

                                asc(member)

                        ))

                        // all aggregates >> single record >> no order

                        : projection.values().stream().anyMatch(not(Expression::aggregate)) ?

                        order(items(

                                // focus values

                                items(query.focus().entrySet().stream()
                                        .map(entry -> {

                                            final Expression expression=entry.getKey();
                                            final Set<Object> values=entry.getValue();

                                            final String alias=null; // !!!

                                            return focus(alias, expression, values, base);

                                        })
                                        .collect(toList())
                                ),

                                // explicit criteria

                                items(query.order().entrySet().stream()
                                        .map(entry -> {

                                            final Criterion criterion=entry.getValue();
                                            final Expression expression=entry.getKey();

                                            final String alias=null; // !!!

                                            return order(alias, expression, criterion);

                                        })
                                        .collect(toList())
                                ),

                                // default criteria // !!! exclude projected values already ordered

                                items(projection.keySet().stream()
                                        .map(x -> var(id(x))) // !!!
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


    private static Expression expand(final Map<List<String>, Expression> projection, final Expression expression) {
        return Optional.ofNullable(projection.get(expression.path()))
                .map(projected -> Stash.expression(
                        projected.path(),
                        Stream.of(expression.transforms(), projected.transforms())
                                .flatMap(Collection::stream)
                                .collect(toList())
                ))
                .orElse(expression);
    }


    private Coder projection(final Map<List<String>, Expression> projection) {
        return items(projection.entrySet().stream()
                .map(entry -> {

                    final List<String> alias=entry.getKey();
                    final Expression expression=entry.getValue();

                    return expression.aggregate() ? as(id(alias), expression(expression))
                            : expression.computed() ? var(id(expression))
                            : var(id(expression.path()));

                })
                .collect(toList())
        );
    }


    private Coder filter(final Collection<Coder> coders) {
        return coders.isEmpty() ? nothing() : filter(indent(list("\n&& ", coders)));
    }

    private Coder having(final Collection<Coder> coders) {
        return coders.isEmpty() ? nothing() : having(indent(list("\n&& ", coders)));
    }

    private Coder focus(final String alias, final Expression expression, final Set<Object> values, final URI base) {

        final Coder result=alias != null ? var(alias) : result(expression);

        final boolean nulls=values.stream().anyMatch(Objects::isNull);
        final boolean nonNulls=values.stream().anyMatch(Objects::nonNull);

        final Coder nb=not(bound(result));

        final Coder in=in(result, values.stream()
                .filter(Objects::nonNull)
                .map(v -> value(v, base))
                .collect(toList())
        );

        return desc(nulls && nonNulls ? or(nb, in)
                : nonNulls ? and(bound(result), in)
                : nb
        );
    }

    private Coder order(final String alias, final Expression expression, final Criterion criterion) {

        final Coder result=alias != null ? var(alias) : result(expression);

        return criterion == increasing ? asc(result) : desc(result);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder path(final List<String> path) {
        return list("/", path.stream()
                .map(this::iri)
                .collect(toList())
        );
    }

    private Coder result(final Expression expression) {
        return var(id(expression.computed() ? expression : expression.path()));
    }

    private Coder expression(final Expression expression) {
        return transform(
                expression.transforms(),
                expression.path().isEmpty() ? text("*") : var(id(expression.path()))
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
        return lt(value, value(limit, base));
    }

    private Coder gt(final Coder value, final Object limit, final URI base) {
        return gt(value, value(limit, base));
    }


    private Coder lte(final Coder value, final Object limit, final URI base) {
        return lte(value, value(limit, base));
    }

    private Coder gte(final Coder value, final Object limit, final URI base) {
        return gte(value, value(limit, base));
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
