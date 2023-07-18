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

import com.metreeca.link.Shape;
import com.metreeca.link.specs.*;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.link.Shape.direct;
import static com.metreeca.link.Shape.forward;
import static com.metreeca.link.rdf4j.Coder.*;
import static com.metreeca.link.specs.Constraint.pattern;
import static com.metreeca.link.specs.Criterion.increasing;
import static com.metreeca.link.specs.Transform.count;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class SPARQLSelector extends SPARQL {

    private static final int DefaultLimit=100;

    private static final Expression Root=Expression.expression("");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Collection<Task<Object, List<Value>>> objects=new ArrayList<>();
    private final Collection<Task<Table, List<Map<String, Value>>>> tables=new ArrayList<>();


    CompletableFuture<List<Value>> select(
            final Resource resource,
            final Optional<String> predicate,
            final Shape shape,
            final Specs specs,
            final Object model
    ) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( specs == null ) {
            throw new NullPointerException("null specs");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Task<Object, List<Value>>(resource, predicate, shape, specs, model).schedule(objects::add);

    }

    CompletableFuture<List<Map<String, Value>>> select(
            final Resource resource,
            final Optional<String> predicate,
            final Shape shape,
            final Specs specs,
            final Table model
    ) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( specs == null ) {
            throw new NullPointerException("null specs");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Task<Table, List<Map<String, Value>>>(resource, predicate, shape, specs, model).schedule(tables::add);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override Optional<CompletableFuture<Void>> run(final RepositoryConnection connection) { // !!! batch execution

        final CompletableFuture<?>[] futures=Stream

                .concat(

                        snapshot(objects).stream().map(task -> CompletableFuture.runAsync(() -> {

                            final Specs specs=task.specs;

                            final String query=sparql(members(
                                    task.resource, task.predicate, task.shape, specs, null
                            ));

                            try ( final Stream<BindingSet> results=connection.prepareTupleQuery(query).evaluate().stream() ) {

                                task.complete(results
                                        .map(bindings -> bindings.getValue(id()))
                                        .collect(toList())
                                );

                            }

                        })),

                        snapshot(tables).stream().map(task -> CompletableFuture.runAsync(() -> {

                            final Specs specs=task.specs;
                            final Table table=task.model;

                            final String query=sparql(members(
                                    task.resource, task.predicate, task.shape, specs, table
                            ));

                            try ( final Stream<BindingSet> results=connection.prepareTupleQuery(query).evaluate().stream() ) {

                                task.complete(results
                                        .map((Function<BindingSet, Map<String, Value>>)bindings ->
                                                table.entrySet().stream().collect(
                                                        HashMap::new, // ;( handle null values
                                                        (map, entry) -> map.put(
                                                                entry.getKey(),
                                                                bindings.getValue(id(entry.getKey(), entry.getValue()))
                                                        ),
                                                        Map::putAll
                                                )
                                        )
                                        .collect(toList())
                                );

                            }

                        }))

                )

                .toArray(CompletableFuture[]::new);

        return futures.length == 0 ? Optional.empty() : Optional.of(allOf(futures));

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder members(
            final Resource container,
            final Optional<String> predicate,
            final Shape shape,
            final Specs specs,
            final Map<String, Column> columns
    ) {

        final URI base=URI.create(container.isIRI() ? container.stringValue() : Shape.DefaultBase);

        final boolean plain=columns == null;

        final Map<String, Expression> projection=plain ? Map.of() : expressions(columns);

        final Map<Expression, Constraint> filter=resolve(projection, specs.filter());
        final Map<Expression, Set<Object>> focus=resolve(projection, specs.focus());
        final Map<Expression, Criterion> order=resolve(projection, specs.order());

        final boolean grouping=(

                projection.isEmpty()
                        || projection.values().stream().anyMatch(Predicate.not(Expression::aggregate))

        ) && (

                projection.values().stream().anyMatch(Expression::aggregate)
                        || filter.keySet().stream().anyMatch(Expression::aggregate)
                        || order.keySet().stream().anyMatch(Expression::aggregate)
                        || focus.keySet().stream().anyMatch(Expression::aggregate)

        );

        final Coder member=var(id());

        return items(

                SPARQL.select(plain, plain ? member : projection(projection)),

                space(where(space(

                        // collection membership

                        predicate
                                .map(iri -> direct(iri)
                                        ? edge(value(container), iri(iri), member)
                                        : edge(member, iri(forward(iri)), value(container))
                                )
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

                                .filter(Predicate.not(List::isEmpty)) // not referring to the root value

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

                                .filter(Predicate.not(Expression::aggregate))
                                .filter(Expression::computed)

                                .map(expression -> line(bind(expression(expression), id(expression))))
                                .collect(toList())
                        )),

                        // non-aggregate filters

                        space(filter(filter.entrySet().stream()
                                .filter(Predicate.not(entry -> entry.getKey().aggregate()))
                                .flatMap(entry -> constraint(var(id(entry.getKey())), entry.getValue(), base))
                                .collect(toList()))
                        )

                ))),

                // grouping

                space(grouping ?

                        groupBy(plain ? member : items(projection.values().stream()
                                .filter(Predicate.not(Expression::aggregate))
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

                        : projection.values().stream().anyMatch(Predicate.not(Expression::aggregate)) ?

                        orderBy(items(

                                focus(base, focus), // focus values
                                order(order), // explicit criteria

                                items(projection.entrySet().stream() // default criteria
                                        .filter(Predicate.not(entry -> order.containsKey(entry.getValue())))
                                        .map(entry -> asc(var(id(entry.getKey(), entry.getValue()))))
                                        .collect(toList())
                                )

                        ))

                        // all aggregates >> single record >> no order

                        : nothing()

                ),

                // range

                space(
                        line(offset(specs.offset())),
                        line(limit(Optional.of(specs.limit()).filter(v -> v > 0).orElse(DefaultLimit)))
                )

        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String id() {
        return id(Root);
    }

    private String id(final String alias, final Column column) {
        return id(alias, column.expression());
    }


    private String id(final String alias, final Expression expression) {
        return expression.aggregate() ? id(alias) : id(expression);
    }

    private String id(final Expression expression) {
        return id(expression.computed() ? expression : expression.path());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Map<String, Expression> expressions(final Map<String, Column> columns) {

        final Map<String, Expression> expressions=new LinkedHashMap<>();

        columns.forEach((alias, column) -> expressions.put(alias, column.expression()));

        return expressions;
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
                .map(projected -> Expression.expression(
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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Task<V, F> {

        final Resource resource;
        final Optional<String> predicate;
        final Shape shape;
        final Specs specs;
        final V model;

        private final CompletableFuture<F> future=new CompletableFuture<>();


        private Task(
                final Resource resource,
                final Optional<String> predicate,
                final Shape shape,
                final Specs specs,
                final V model
        ) {
            this.resource=resource;
            this.predicate=predicate;
            this.shape=shape;
            this.specs=specs;
            this.model=model;
        }


        private CompletableFuture<F> schedule(final Consumer<Task<V, F>> queue) {

            queue.accept(this);

            return future;
        }

        private void complete(final F value) {
            future.complete(value);
        }

    }

}
