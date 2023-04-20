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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Shape.forward;
import static com.metreeca.link.Shape.reverse;
import static com.metreeca.link.rdf4j.Coder.*;

import static org.eclipse.rdf4j.model.util.Values.literal;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * SPARQL query generator.
 */
abstract class SPARQL {

    private static final Logger logger=Logger.getLogger(SPARQL.class.getName());


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Object, String> scope=new HashMap<>();


    String id() {
        return id(List.of());
    }

    String id(final Object object) {
        return scope.computeIfAbsent(object, o -> String.valueOf(scope.size()));
    }


    String query(final Coder query) {

        final String code=query.toString();

        logger.info(() -> code); // !!! debug

        return code;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder comment(final String text) {
        return space(text("# ", text));
    }


    Coder base(final String base) {
        return space(items(text("base <", base, ">")));
    }

    Coder prefix(final String prefix, final String name) {
        return line(text("prefix ", prefix, ": <", name, ">"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder select(final Coder... expressions) {
        return select(false, expressions);
    }

    Coder select(final boolean distinct, final Coder... expressions) {
        return items(text("\rselect"),
                distinct ? text(" distinct") : nothing(),
                expressions.length == 0 ? text(" *") : items(expressions)
        );
    }


    Coder construct(final Coder... patterns) {
        return items(text("\rconstruct"), items(patterns));
    }


    Coder where(final Coder... pattern) {
        return items(text("\rwhere"), block(pattern));
    }


    Coder group(final Coder... expressions) {
        return group(asList(expressions));
    }

    Coder group(final Collection<Coder> expressions) {
        return expressions.isEmpty() ? nothing() : items(text("\rgroup by"), items(expressions));
    }


    Coder having(final Coder expression) {
        return items(text("\rhaving ( "), items(expression), text(" )"));
    }


    Coder order(final Coder... expressions) {
        return items(text(" order by"), items(expressions));
    }

    Coder sort(final boolean inverse, final Coder expression) {
        return inverse ? desc(expression) : asc(expression);
    }

    Coder asc(final Coder expression) {
        return items(text(" asc("), expression, text(")"));
    }

    Coder desc(final Coder expression) {
        return items(text(" desc("), expression, text(")"));
    }


    Coder offset(final int offset) {
        return offset > 0
                ? items(text(" offset "), text(String.valueOf(offset)))
                : nothing();
    }

    Coder limit(final int limit) {
        return limit > 0
                ? items(text(" limit "), text(String.valueOf(limit)))
                : nothing();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder optional(final Coder pattern) {
        return items(text(" optional"), block(pattern));
    }


    Coder union(final Coder... patterns) {
        return union(List.of(patterns));
    }

    Coder union(final Collection<Coder> patterns) {
        return list(" union ", patterns.stream().map(Coder::block).collect(toList()));
    }


    Coder edge(final Coder source, final String predicate, final Coder target) {
        return forward(predicate)
                ? edge(source, iri(predicate), target)
                : edge(target, iri(reverse(predicate)), source);
    }

    Coder edge(final Coder source, final Coder path, final Coder target) {
        return items(text(' '), source, text(' '), path, text(' '), target, text(" ."));
    }


    Coder resource(final Resource resource) {
        return resource.isIRI() ? iri((IRI)resource)
                : resource.isBNode() ? bnode((BNode)resource)
                : nothing();
    }

    Coder bnode(final BNode bnode) {
        return items(text("_:"), text(bnode.getID()));
    }

    Coder iri(final IRI iri) {
        return items(text('<'), text(iri.stringValue()), text('>'));
    }

    Coder iri(final String iri) {
        return forward(iri)
                ? items(text('<'), text(iri), text('>'))
                : items(text("^<"), text(reverse(iri)), text('>'));
    }


    Coder filter(final Coder... expressions) {
        return items(text(" filter ( "), items(expressions), text(" )"));
    }

    Coder in(final Coder expression, final Collection<Coder> expressions) {
        return items(expression, text(" in ("), list(", ", expressions), text(')'));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder eq(final Coder x, final Coder y) {
        return op(x, "=", y);
    }

    Coder neq(final Coder x, final Coder y) {
        return op(x, "!=", y);
    }

    Coder lt(final Coder x, final Coder y) {
        return op(x, "<", y);
    }

    Coder gt(final Coder x, final Coder y) {
        return op(x, ">", y);
    }

    Coder lte(final Coder x, final Coder y) {
        return op(x, "<=", y);
    }

    Coder gte(final Coder x, final Coder y) {
        return op(x, ">=", y);
    }


    Coder or(final Coder... expressions) {
        return and(asList(expressions));
    }

    Coder or(final Collection<Coder> expressions) {
        return list(" || ", expressions);
    }

    Coder and(final Coder... expressions) {
        return and(asList(expressions));
    }

    Coder and(final Collection<Coder> expressions) {
        return list(" && ", expressions);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder is(final Coder test, final Coder pass, final Coder fail) {
        return items(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
    }

    Coder isBlank(final Coder expression) {
        return function("isBlank", expression);
    }

    Coder isIRI(final Coder expression) {
        return function("isIRI", expression);
    }

    Coder isLiteral(final Coder expression) {
        return function("isLiteral", expression);
    }

    Coder bound(final Coder expression) {
        return function("bound", expression);
    }

    Coder lang(final Coder expression) {
        return function("lang", expression);
    }

    Coder datatype(final Coder expression) {
        return function("datatype", expression);
    }

    Coder str(final Coder expression) {
        return function("str", expression);
    }

    Coder strlen(final Coder expression) {
        return function("strlen", expression);
    }

    Coder strstarts(final Coder expression, final Coder prefix) {
        return function("strstarts", expression, prefix);
    }

    Coder regex(final Coder expression, final Coder pattern) {
        return function("regex", expression, pattern);
    }

    Coder regex(final Coder expression, final Coder pattern, final Coder flags) {
        return function("regex", expression, pattern, flags);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder min(final Coder expression) {
        return items(text(" min("), expression, text(")"));
    }

    Coder max(final Coder expression) {
        return items(text(" max("), expression, text(")"));
    }

    Coder count(final Coder expression) {
        return count(false, expression);
    }

    Coder count(final boolean distinct, final Coder expression) {
        return items(text(" count("), distinct ? text("distinct ") : nothing(), expression, text(")"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder op(final Coder x, final String name, final Coder y) {
        return items(x, text(' '), text(name), text(' '), y);
    }

    Coder function(final String name, final Coder... args) {
        return items(text(' '), text(name), text('('), list(", ", args), text(')'));
    }


    Coder bind(final String id, final Coder expression) {
        return items(text(" bind"), as(id, expression));
    }

    Coder as(final String id, final Coder expression) {
        return items(text(" ("), expression, text(" as "), var(id), text(')'));
    }

    Coder var(final String id) {
        return text(" ?", id);
    }


    //// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    Coder value(final Object value, final URI base) { // !!! integrate with RDF4JCodec
        return value(Optional.ofNullable(frame(value).id())
                .map(id -> base.resolve(id).toString())
                .<Value>map(Values::iri)
                .orElseGet(() -> literal(value))
        );
    }

    Coder value(final Value value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return value instanceof BNode ? value((BNode)value)
                : value instanceof IRI ? value((IRI)value)
                : value((Literal)value);
    }

    Coder value(final BNode bnode) {

        if ( bnode == null ) {
            throw new NullPointerException("null bnode");
        }

        return text("_:", bnode.getID());
    }

    Coder value(final IRI iri) {

        if ( iri == null ) {
            throw new NullPointerException("null iri");
        }

        return iri.equals(RDF.TYPE) ? text("a") : text("<", iri.stringValue(), ">");
    }

    Coder value(final Literal literal) {

        if ( literal == null ) {
            throw new NullPointerException("null literal");
        }

        final IRI type=literal.getDatatype();

        return type.equals(XSD.BOOLEAN) ? text(String.valueOf(literal.booleanValue()))

                : type.equals(XSD.INTEGER) ? text(String.valueOf(literal.integerValue()))
                : type.equals(XSD.DECIMAL) ? text(literal.decimalValue().toPlainString())

                : type.equals(XSD.DOUBLE) ? text(format("%e", literal.doubleValue()))
                : type.equals(XSD.STRING) ? quoted(literal.getLabel())

                : literal.getLanguage()
                .map(lang -> items(quoted(literal.getLabel()), text("@"), text(lang)))
                .orElseGet(() -> items(quoted(literal.getLabel()), text("^^"), value(type)));

    }

}
