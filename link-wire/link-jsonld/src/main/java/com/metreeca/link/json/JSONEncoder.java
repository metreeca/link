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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static com.metreeca.link.Frame.*;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.groupingBy;

final class JSONEncoder {

    private static final Set<IRI> RESOURCES=Set.of(RESOURCE, BNODE, IRI);

    private static final ThreadLocal<DecimalFormat> DOUBLE_FORMAT=ThreadLocal.withInitial(() ->
            new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(ROOT)) // ;( not thread-safe
    );


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final URI base;
    private final JSONWriter writer;


    JSONEncoder(final JSON json, final URI base, final Appendable target) {
        this.base=base;
        this.writer=new JSONWriter(json, target);
    }


    void encode(final Shape shape, final Frame frame) {
        value(shape, frame);
    }

    void encode(final Shape shape, final Trace trace) {

        writer.object(true);

        if ( !trace.errors().isEmpty() ) {

            writer.string(_ERRORS);
            writer.colon();

            writer.array(true);

            trace.errors().forEach(string -> {

                writer.comma();
                writer.string(string);

            });

            writer.array(false);
        }

        trace.entries().forEach((property, nested) -> shape.entry(property).ifPresent(e -> {

            writer.comma();
            writer.string(e.getKey());
            writer.colon();

            encode(e.getValue(), nested);

        }));

        writer.object(false);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void values(final Shape shape, final Iterable<Value> values) {

        writer.array(true);

        values.forEach(value -> {

            writer.comma();

            value(shape, value);

        });

        writer.array(false);

    }

    private void value(final Shape shape, final Value value) {
        if ( value instanceof Frame ) {

            frame(shape, ((Frame)value));

        } else if ( value.isResource() ) {

            resource(shape, ((Resource)value));

        } else if ( value.isLiteral() ) {

            literal(shape, ((Literal)value));

        }
    }


    private void frame(final Shape shape, final Frame frame) {

        final boolean tabular=frame.tabular();

        writer.object(true);

        frame.fields().forEach((predicate, values) -> shape.entry(predicate).ifPresentOrElse(

                entry -> {

                    writer.comma();

                    field(tabular, entry.getKey(), entry.getValue(), values);

                },

                () -> {

                    if ( predicate instanceof Probe ) {

                        final String label=((Probe)predicate).label();
                        final Expression expression=((Probe)predicate).expression();

                        writer.comma();

                        field(tabular, label, expression.apply(shape), values);

                    }

                }

        ));

        writer.object(false);
    }

    private void field(final boolean tabular, final String label, final Shape shape, final Collection<Value> values) {

        writer.string(label);
        writer.colon();

        if ( shape.datatype().filter(RDF.LANGSTRING::equals).isPresent() ) {

            locals(shape, values);

        } else if ( tabular || shape.maxCount().filter(maxCount -> maxCount == 1).isPresent() ) {

            values.stream().findFirst().ifPresent(value -> value(shape, value));

        } else {

            values(shape, values);

        }

    }

    private void resource(final Shape shape, final Resource resource) {

        if ( resource.equals(NIL) ) {

            writer.literal("null");

        } else {

            final String id=resource.isBNode()
                    ? format("_:%s", resource.stringValue())
                    : relativize(resource.stringValue());

            if ( shape.datatype().filter(RESOURCES::contains).isPresent() ) {

                writer.string(id);

            } else {

                writer.object(true);

                writer.string(_ID);
                writer.colon();
                writer.string(id);

                writer.object(false);

            }

        }

    }

    private void literal(final Shape shape, final Literal literal) {

        final IRI datatype=literal.getDatatype();

        if ( datatype.equals(XSD.BOOLEAN) ) {

            _boolean(literal);

        } else if ( datatype.equals(XSD.DOUBLE) ) {

            _double(literal);

        } else if ( datatype.equals(XSD.INTEGER) ) {

            integer(literal);

        } else if ( datatype.equals(XSD.DECIMAL) ) {

            decimal(literal);

        } else if ( datatype.equals(XSD.STRING) ) {

            string(literal);

        } else if ( datatype.equals(RDF.LANGSTRING) ) {

            tagged(literal);

        } else {

            if ( shape.datatype().filter(datatype::equals).isPresent() ) {

                writer.string(literal.stringValue());

            } else {

                typed(literal);

            }

        }
    }


    private void _boolean(final Literal literal) {
        writer.literal(literal.booleanValue() ? "true" : "false");
    }

    private void _double(final Literal literal) {
        writer.literal(DOUBLE_FORMAT.get().format(literal.doubleValue()));
    }

    private void integer(final Literal literal) {
        writer.literal(literal.integerValue().toString());
    }

    private void decimal(final Literal literal) {
        writer.literal(literal.decimalValue().toPlainString());
    }

    private void string(final Literal literal) {
        writer.string(literal.stringValue());
    }

    private void tagged(final Literal literal) {

        writer.object(true);

        writer.string(_VALUE);
        writer.colon();
        writer.string(literal.stringValue());

        writer.comma();

        writer.string(_LANGUAGE);
        writer.colon();
        writer.string(literal.getLanguage().orElseThrow(() ->
                new IllegalArgumentException("missing language tag") // unexpected
        ));

        writer.object(false);
    }

    private void typed(final Literal literal) {

        writer.object(true);

        writer.string(_VALUE);
        writer.colon();
        writer.string(literal.stringValue());

        writer.comma();

        writer.string(_TYPE);
        writer.colon();
        writer.string(literal.getDatatype().stringValue());

        writer.object(false);
    }


    private void locals(final Shape shape, final Collection<Value> values) {

        final boolean unique=shape.maxCount().filter(maxCount -> maxCount == 1).isPresent();

        writer.object(true);

        values.stream()

                .filter(Value::isLiteral)
                .map(Literal.class::cast)

                .collect(groupingBy(value -> value.getLanguage().orElse(ROOT.getLanguage())))

                .forEach((locale, literals) -> {

                    if ( !locale.equals(ROOT.getLanguage()) ) { // ignore untagged literals

                        writer.comma();
                        writer.string(locale);
                        writer.colon();

                        if ( unique ) {

                            literals.stream().findFirst().ifPresent(literal -> writer.string(literal.stringValue()));

                        } else {

                            writer.array(true);

                            literals.forEach(literal -> {

                                writer.comma();
                                writer.string(literal.stringValue());

                            });

                            writer.array(false);
                        }

                    }

                });

        writer.object(false);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String relativize(final String uri) {
        return relativize(URI.create(uri)).toASCIIString();
    }

    private URI relativize(final URI uri) {
        if ( Objects.equals(base.getScheme(), uri.getScheme())
                && Objects.equals(base.getRawAuthority(), uri.getRawAuthority())
        ) {

            return URI.create(uri.getRawSchemeSpecificPart()
                    .substring(uri.getRawAuthority().length()+2)
            );

        } else {

            return uri;

        }
    }

}
