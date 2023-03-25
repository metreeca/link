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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

import static com.metreeca.rest.Shape.forward;
import static com.metreeca.rest.Shape.reverse;
import static com.metreeca.rest.rdf4j.Coder.*;

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


    String query(final com.metreeca.rest.rdf4j.Coder query) {

        final String code=query.toString();

        logger.info(() -> code); // !!! debug

        return code;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder comment(final String text) {
        return space(text("# ", text));
    }


    com.metreeca.rest.rdf4j.Coder base(final String base) {
        return space(items(text("base <", base, ">")));
    }

    com.metreeca.rest.rdf4j.Coder prefix(final String prefix, final String name) {
        return line(text("prefix ", prefix, ": <", name, ">"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder select(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return select(false, expressions);
    }

    com.metreeca.rest.rdf4j.Coder select(final boolean distinct, final com.metreeca.rest.rdf4j.Coder... expressions) {
        return items(text("\rselect"),
                distinct ? text(" distinct") : nothing(),
                expressions.length == 0 ? text(" *") : items(expressions)
        );
    }


    com.metreeca.rest.rdf4j.Coder construct(final com.metreeca.rest.rdf4j.Coder... patterns) {
        return items(text("\rconstruct"), items(patterns));
    }


    com.metreeca.rest.rdf4j.Coder where(final com.metreeca.rest.rdf4j.Coder... pattern) {
        return items(text("\rwhere"), block(pattern));
    }


    com.metreeca.rest.rdf4j.Coder group(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return group(asList(expressions));
    }

    com.metreeca.rest.rdf4j.Coder group(final Collection<com.metreeca.rest.rdf4j.Coder> expressions) {
        return expressions.isEmpty() ? nothing() : items(text("\rgroup by"), items(expressions));
    }


    com.metreeca.rest.rdf4j.Coder having(final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text("\rhaving ( "), items(expression), text(" )"));
    }


    com.metreeca.rest.rdf4j.Coder order(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return items(text(" order by"), items(expressions));
    }

    com.metreeca.rest.rdf4j.Coder sort(final boolean inverse, final com.metreeca.rest.rdf4j.Coder expression) {
        return inverse ? desc(expression) : asc(expression);
    }

    com.metreeca.rest.rdf4j.Coder asc(final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" asc("), expression, text(")"));
    }

    com.metreeca.rest.rdf4j.Coder desc(final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" desc("), expression, text(")"));
    }


    com.metreeca.rest.rdf4j.Coder offset(final int offset) {
        return offset > 0
                ? items(text(" offset "), text(String.valueOf(offset)))
                : nothing();
    }

    com.metreeca.rest.rdf4j.Coder limit(final int limit) {
        return limit > 0
                ? items(text(" limit "), text(String.valueOf(limit)))
                : nothing();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder optional(final com.metreeca.rest.rdf4j.Coder pattern) {
        return items(text(" optional"), block(pattern));
    }


    com.metreeca.rest.rdf4j.Coder union(final com.metreeca.rest.rdf4j.Coder... patterns) {
        return union(List.of(patterns));
    }

    com.metreeca.rest.rdf4j.Coder union(final Collection<com.metreeca.rest.rdf4j.Coder> patterns) {
        return list(" union ", patterns.stream().map(com.metreeca.rest.rdf4j.Coder::block).collect(toList()));
    }


    com.metreeca.rest.rdf4j.Coder edge(final com.metreeca.rest.rdf4j.Coder source, final String predicate,
            final com.metreeca.rest.rdf4j.Coder target) {
        return forward(predicate)
                ? edge(source, iri(predicate), target)
                : edge(target, iri(reverse(predicate)), source);
    }

    com.metreeca.rest.rdf4j.Coder edge(final com.metreeca.rest.rdf4j.Coder source,
            final com.metreeca.rest.rdf4j.Coder path, final com.metreeca.rest.rdf4j.Coder target) {
        return items(text(' '), source, text(' '), path, text(' '), target, text(" ."));
    }


    com.metreeca.rest.rdf4j.Coder resource(final Resource resource) {
        return resource.isIRI() ? iri((IRI)resource)
                : resource.isBNode() ? bnode((BNode)resource)
                : nothing();
    }

    com.metreeca.rest.rdf4j.Coder bnode(final BNode bnode) {
        return items(text("_:"), text(bnode.getID()));
    }

    com.metreeca.rest.rdf4j.Coder iri(final IRI iri) {
        return items(text('<'), text(iri.stringValue()), text('>'));
    }

    com.metreeca.rest.rdf4j.Coder iri(final String iri) {
        return forward(iri)
                ? items(text('<'), text(iri), text('>'))
                : items(text("^<"), text(reverse(iri)), text('>'));
    }


    com.metreeca.rest.rdf4j.Coder filter(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return items(text(" filter ( "), items(expressions), text(" )"));
    }

    com.metreeca.rest.rdf4j.Coder in(final com.metreeca.rest.rdf4j.Coder expression,
            final Collection<com.metreeca.rest.rdf4j.Coder> expressions) {
        return items(expression, text(" in ("), list(", ", expressions), text(')'));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder eq(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, "=", y);
    }

    com.metreeca.rest.rdf4j.Coder neq(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, "!=", y);
    }

    com.metreeca.rest.rdf4j.Coder lt(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, "<", y);
    }

    com.metreeca.rest.rdf4j.Coder gt(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, ">", y);
    }

    com.metreeca.rest.rdf4j.Coder lte(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, "<=", y);
    }

    com.metreeca.rest.rdf4j.Coder gte(final com.metreeca.rest.rdf4j.Coder x, final com.metreeca.rest.rdf4j.Coder y) {
        return op(x, ">=", y);
    }


    com.metreeca.rest.rdf4j.Coder or(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return and(asList(expressions));
    }

    com.metreeca.rest.rdf4j.Coder or(final Collection<com.metreeca.rest.rdf4j.Coder> expressions) {
        return list(" || ", expressions);
    }

    com.metreeca.rest.rdf4j.Coder and(final com.metreeca.rest.rdf4j.Coder... expressions) {
        return and(asList(expressions));
    }

    com.metreeca.rest.rdf4j.Coder and(final Collection<com.metreeca.rest.rdf4j.Coder> expressions) {
        return list(" && ", expressions);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder is(final com.metreeca.rest.rdf4j.Coder test,
            final com.metreeca.rest.rdf4j.Coder pass, final com.metreeca.rest.rdf4j.Coder fail) {
        return items(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
    }

    com.metreeca.rest.rdf4j.Coder isBlank(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("isBlank", expression);
    }

    com.metreeca.rest.rdf4j.Coder isIRI(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("isIRI", expression);
    }

    com.metreeca.rest.rdf4j.Coder isLiteral(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("isLiteral", expression);
    }

    com.metreeca.rest.rdf4j.Coder bound(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("bound", expression);
    }

    com.metreeca.rest.rdf4j.Coder lang(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("lang", expression);
    }

    com.metreeca.rest.rdf4j.Coder datatype(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("datatype", expression);
    }

    com.metreeca.rest.rdf4j.Coder str(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("str", expression);
    }

    com.metreeca.rest.rdf4j.Coder strlen(final com.metreeca.rest.rdf4j.Coder expression) {
        return function("strlen", expression);
    }

    com.metreeca.rest.rdf4j.Coder strstarts(final com.metreeca.rest.rdf4j.Coder expression,
            final com.metreeca.rest.rdf4j.Coder prefix) {
        return function("strstarts", expression, prefix);
    }

    com.metreeca.rest.rdf4j.Coder regex(final com.metreeca.rest.rdf4j.Coder expression,
            final com.metreeca.rest.rdf4j.Coder pattern) {
        return function("regex", expression, pattern);
    }

    com.metreeca.rest.rdf4j.Coder regex(final com.metreeca.rest.rdf4j.Coder expression,
            final com.metreeca.rest.rdf4j.Coder pattern, final com.metreeca.rest.rdf4j.Coder flags) {
        return function("regex", expression, pattern, flags);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder min(final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" min("), expression, text(")"));
    }

    com.metreeca.rest.rdf4j.Coder max(final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" max("), expression, text(")"));
    }

    com.metreeca.rest.rdf4j.Coder count(final com.metreeca.rest.rdf4j.Coder expression) {
        return count(false, expression);
    }

    com.metreeca.rest.rdf4j.Coder count(final boolean distinct, final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" count("), distinct ? text("distinct ") : nothing(), expression, text(")"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder op(final com.metreeca.rest.rdf4j.Coder x, final String name,
            final com.metreeca.rest.rdf4j.Coder y) {
        return items(x, text(' '), text(name), text(' '), y);
    }

    com.metreeca.rest.rdf4j.Coder function(final String name, final com.metreeca.rest.rdf4j.Coder... args) {
        return items(text(' '), text(name), text('('), list(", ", args), text(')'));
    }


    com.metreeca.rest.rdf4j.Coder bind(final String id, final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" bind"), as(id, expression));
    }

    com.metreeca.rest.rdf4j.Coder as(final String id, final com.metreeca.rest.rdf4j.Coder expression) {
        return items(text(" ("), expression, text(" as "), var(id), text(')'));
    }

    com.metreeca.rest.rdf4j.Coder var(final String id) {
        return text(" ?", id);
    }


    //// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    com.metreeca.rest.rdf4j.Coder encode(final Object value) { // !!! integrate with RDF4JCodec
        return value instanceof Boolean ? text(String.valueOf(value))

                : value instanceof BigInteger ? text(String.valueOf(value))
                : value instanceof BigDecimal ? text(String.valueOf(value))

                : value instanceof Float ? text(format("%e", value))
                : value instanceof Double ? text(format("%e", value))

                : value instanceof Number ? text(String.valueOf(value)) // !!! review

                : value(literal(value.toString()));
    }


    com.metreeca.rest.rdf4j.Coder values(final Collection<Value> values) {
        return list(", ", values.stream().map(this::value).collect(toList()));
    }


    com.metreeca.rest.rdf4j.Coder value(final Value value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return value instanceof BNode ? value((BNode)value)
                : value instanceof IRI ? value((IRI)value)
                : value((Literal)value);
    }

    com.metreeca.rest.rdf4j.Coder value(final BNode bnode) {

        if ( bnode == null ) {
            throw new NullPointerException("null bnode");
        }

        return text("_:", bnode.getID());
    }

    com.metreeca.rest.rdf4j.Coder value(final IRI iri) {

        if ( iri == null ) {
            throw new NullPointerException("null iri");
        }

        return iri.equals(RDF.TYPE) ? text("a") : text("<", iri.stringValue(), ">");
    }

    com.metreeca.rest.rdf4j.Coder value(final Literal literal) {

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
