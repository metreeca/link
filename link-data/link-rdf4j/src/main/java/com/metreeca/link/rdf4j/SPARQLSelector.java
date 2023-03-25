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

package com.metreeca.link.rdf4j;

import com.metreeca.link.Constraint;
import com.metreeca.link.Expression;
import com.metreeca.link.Query;
import com.metreeca.link.Transform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.metreeca.link.Constraint.pattern;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Transform.COUNT;
import static com.metreeca.link.rdf4j.Coder.*;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Predicate.not;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.*;

final class SPARQLSelector extends SPARQL {

    private static final int DEFAULT_LIMIT=100;

    private static final Expression ROOT=Expression.expression(List.of(), List.of());


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Collection<Task<List<Value>>> values=new ArrayList<>();
    private final Collection<Task<List<Map<IRI, Value>>>> tuples=new ArrayList<>();


    CompletableFuture<List<Value>> select(
            final Resource id,
            final IRI property,
            final boolean virtual,
            final Query query
    ) {

        return new Task<List<Value>>(id, property, virtual, query, Map.of()).schedule(values::add);

    }

    CompletableFuture<List<Map<IRI, Value>>> select(
            final Resource id,
            final IRI property,
            final boolean virtual,
            final Query query,
            final Map<IRI, Expression> fields
    ) {

        return new Task<List<Map<IRI, Value>>>(id, property, virtual, query, fields).schedule(tuples::add);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override Optional<CompletableFuture<Void>> run(final RepositoryConnection connection) { // !!! batch execution

        final CompletableFuture<?>[] futures=Stream

                .concat(

                        snapshot(values).stream().map(task -> CompletableFuture.runAsync(() -> {

                            try ( final Stream<BindingSet> results=connection.prepareTupleQuery(sparql(members(

                                    task.id, task.property, task.virtual, task.query, task.fields

                            ))).evaluate().stream() ) {

                                task.complete(results.map(bindings ->

                                        bindings.getValue(id())

                                ).collect(toList()));

                            }

                        })),

                        snapshot(tuples).stream().map(task -> CompletableFuture.runAsync(() -> {

                            try ( final Stream<BindingSet> results=connection.prepareTupleQuery(sparql(members(

                                    task.id, task.property, task.virtual, task.query, task.fields

                            ))).evaluate().stream() ) {

                                task.complete(results.map(bindings ->

                                        task.fields.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {

                                            final Value value=bindings.getValue(id(e.getKey(), e.getValue()));

                                            return value != null ? value : NIL;

                                        }))

                                ).collect(toList()));

                            }

                        }))

                )

                .toArray(CompletableFuture[]::new);

        return futures.length == 0 ? Optional.empty() : Optional.of(allOf(futures));

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder members(
            final Resource id,
            final IRI property,
            final boolean virtual,
            final Query query,
            final Map<IRI, Expression> fields
    ) {

        final boolean plain=fields.isEmpty();

        final Map<Expression, Constraint> filter=query.filter();
        final Map<Expression, Set<Value>> focus=query.focus();
        final Map<Expression, Integer> order=query.order();

        final boolean grouping=(

                fields.isEmpty()
                        || fields.values().stream().anyMatch(not(Expression::aggregate))

        ) && (

                fields.values().stream().anyMatch(Expression::aggregate)
                        || filter.keySet().stream().anyMatch(Expression::aggregate)
                        || order.keySet().stream().anyMatch(Expression::aggregate)
                        || focus.keySet().stream().anyMatch(Expression::aggregate)

        );

        final Coder member=var(id());


        return items(

                select(plain, plain ? member : projection(fields)),

                space(where(space(

                        // collection membership

                        virtual ? nothing() : forward(property)
                                ? edge(value(id), iri(property), member)
                                : edge(member, iri(reverse(property)), value(id)),

                        // non-aggregate filters

                        items(filter.entrySet().stream()

                                .filter(not(entry -> entry.getKey().aggregate()))

                                .map(entry -> { // !!! refactor

                                    final Expression expression=entry.getKey();
                                    final Constraint constraint=entry.getValue();

                                    final List<Transform> pipe=expression.pipe();
                                    final List<IRI> path=expression.path();

                                    if ( path.isEmpty() ) {

                                        return space(
                                                filter(constraint(transform(pipe, member), constraint))
                                        );

                                    } else {

                                        final boolean nil=constraint.any().stream()
                                                .flatMap(Collection::stream)
                                                .anyMatch(NIL::equals);

                                        final Coder var=var(id(path));
                                        final Coder edge=edge(member, path, var);

                                        return space(
                                                nil ? optional(edge) : edge,
                                                filter(constraint(transform(pipe, var), constraint))
                                        );

                                    }

                                })

                                .collect(toList())
                        ),

                        // plain expression values

                        space(items(Stream

                                .of(
                                        fields.values(),
                                        filter.keySet().stream()
                                                .filter(Expression::aggregate)
                                                .collect(toList()),
                                        focus.keySet(),
                                        order.keySet()
                                )

                                .flatMap(Collection::stream)
                                .map(Expression::path)
                                .distinct()

                                .filter(not(List::isEmpty)) // not referring to root

                                .map(path -> line(optional(edge(member, path, var(id(path))))))

                                .collect(toList())
                        )),

                        // non-aggregate computed values

                        space(items(Stream

                                .of(
                                        fields.values(),
                                        filter.keySet().stream()
                                                .filter(Expression::aggregate)
                                                .collect(toList()),
                                        focus.keySet(),
                                        order.keySet()
                                )

                                .flatMap(Collection::stream)

                                .filter(not(Expression::aggregate))
                                .filter(Expression::computed)

                                .map(expression -> line(bind(expression(expression), id(expression))))
                                .collect(toList())
                        ))

                ))),

                // grouping

                space(grouping ?

                        groupBy(plain ? member : items(fields.values().stream()
                                .filter(not(Expression::aggregate))
                                .map(expression -> var(id(expression)))
                                .collect(toList())
                        ))

                        : nothing()

                ),

                // aggregate filters

                space(having(filter.entrySet().stream()
                        .filter(entry -> entry.getKey().aggregate())
                        .map(entry -> constraint(expression(entry.getKey()), entry.getValue()))
                        .collect(toList()))
                ),

                // ordering

                space(plain ?

                        orderBy(items(

                                focus(focus), // focus values
                                order(order), // explicit criteria

                                order.containsKey(ROOT) ? nothing() : asc(member) // default criteria

                        ))

                        : fields.values().stream().anyMatch(not(Expression::aggregate)) ?

                        orderBy(items(

                                focus(focus), // focus values
                                order(order), // explicit criteria

                                items(fields.entrySet().stream() // default criteria
                                        .filter(not(entry -> order.containsKey(entry.getValue())))
                                        .map(entry -> asc(var(id(entry.getKey(), entry.getValue()))))
                                        .collect(toList())
                                )

                        ))

                        // all aggregates >> single record >> no order

                        : nothing()

                ),

                // slice

                space(
                        line(offset(query.offset())),
                        line(limit(Optional.of(query.limit()).filter(v -> v > 0).orElse(DEFAULT_LIMIT)))
                )

        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String id() {
        return id(ROOT);
    }

    private String id(final IRI property, final Expression expression) {
        return expression.aggregate() ? id(property) : id(expression);
    }

    private String id(final Expression expression) {
        return id(expression.computed() ? expression : expression.path());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder projection(final Map<IRI, Expression> projection) {
        return projection.isEmpty() ? star() : items(projection.entrySet().stream()
                .map(entry -> {

                    final IRI property=entry.getKey();
                    final Expression expression=entry.getValue();

                    return expression.aggregate() ? as(expression(expression), id(property)) : var(id(expression));

                })
                .collect(toList())
        );
    }


    private Coder focus(final Map<Expression, Set<Value>> focus) {
        return items(focus.entrySet().stream()
                .map(entry -> {

                    final Expression expression=entry.getKey();
                    final Set<Value> values=entry.getValue();

                    return focus(values, expression.aggregate()
                            ? expression(expression)
                            : var(id(expression))
                    );

                })
                .collect(toList())
        );
    }

    private Coder order(final Map<Expression, Integer> order) {
        return items(order.entrySet().stream()
                .map(entry -> {

                    final Expression expression=entry.getKey();
                    final Integer criterion=entry.getValue();

                    return order(criterion, expression.aggregate()
                            ? expression(expression)
                            : var(id(expression))
                    );

                })
                .collect(toList())
        );
    }


    private Coder focus(final Collection<Value> values, final Coder value) {

        final boolean nulls=values.stream().anyMatch(Objects::isNull);
        final boolean nonNulls=values.stream().anyMatch(Objects::nonNull);

        final Coder nb=nt(bound(value));

        final Coder in=in(value, values.stream()
                .filter(Objects::nonNull)
                .map(SPARQL::value)
                .collect(toList())
        );

        return desc(nulls && nonNulls ? or(nb, in)
                : nonNulls ? and(bound(value), in)
                : nb
        );
    }

    private Coder order(final Integer criterion, final Coder value) { // !!! priority
        return criterion >= 0 ? asc(value) : desc(value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder expression(final Expression expression) {
        return transform(
                expression.pipe(),
                expression.path().isEmpty() ? star() : var(id(expression.path()))
        );
    }

    private Coder transform(final List<Transform> transforms, final Coder value) {
        if ( transforms.isEmpty() ) { return value; } else {

            final Transform head=transforms.get(0);
            final List<Transform> tail=transforms.subList(1, transforms.size());

            return head == COUNT
                    ? count(true, transform(tail, value))
                    : function(head.name().toLowerCase(Locale.ROOT), transform(tail, value));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder constraint(final Coder value, final Constraint constraint) {
        return and(Stream.

                of(

                        constraint.lt().map(limit -> lt(value, limit)).stream(),
                        constraint.gt().map(limit -> gt(value, limit)).stream(),

                        constraint.lte().map(limit -> lte(value, limit)).stream(),
                        constraint.gte().map(limit -> gte(value, limit)).stream(),

                        constraint.like().stream().map(keywords -> like(value, keywords)),
                        constraint.any().stream().map(values -> any(value, values))

                )

                .flatMap(identity())

                .collect(toList())
        );
    }


    private Coder lt(final Coder value, final Value limit) {
        return SPARQL.lt(value, value(limit));
    }

    private Coder gt(final Coder value, final Value limit) {
        return SPARQL.gt(value, value(limit));
    }


    private Coder lte(final Coder value, final Value limit) {
        return SPARQL.lte(value, value(limit));
    }

    private Coder gte(final Coder value, final Value limit) {
        return SPARQL.gte(value, value(limit));
    }


    private Coder like(final Coder value, final String keywords) {
        return regex(str(value), quoted(pattern(keywords, true)));
    }

    private Coder any(final Coder value, final Collection<Value> values) {

        if ( values.isEmpty() ) {

            return bound(value);

        } else {

            final Set<Value> options=values.stream()
                    .filter(not(v -> v.equals(NIL)))
                    .collect(toSet());

            final Coder negative=nt(bound(value));
            final Coder positive=options.size() == 1
                    ? eq(value, value(options.iterator().next()))
                    : in(value, options.stream().map(SPARQL::value).collect(toList()));

            return values.stream().noneMatch(NIL::equals) ? positive
                    : options.isEmpty() ? negative
                    : parens(or(negative, positive));
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Task<V> {

        final Resource id;
        final IRI property;
        final boolean virtual;
        final Query query;
        final Map<IRI, Expression> fields;

        private final CompletableFuture<V> future=new CompletableFuture<>();


        private Task(
                final Resource id,
                final IRI property,
                final boolean virtual,
                final Query query,
                final Map<IRI, Expression> fields
        ) {
            this.id=id;
            this.property=property;
            this.virtual=virtual;
            this.query=query;
            this.fields=fields;
        }


        private CompletableFuture<V> schedule(final Consumer<Task<V>> queue) {

            queue.accept(this);

            return future;
        }

        private void complete(final V value) {
            future.complete(value);
        }

    }

}
