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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.json.JSONReader.Type.*;
import static com.metreeca.link.json._Parser.*;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

final class JSONDecoder {

    private static final Literal TRUE=literal(true);
    private static final Literal FALSE=literal(false);

    private static final Set<String> TYPED=Set.of(_VALUE, _TYPE);
    private static final Set<String> TAGGED=Set.of(_VALUE, _LANGUAGE);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final URI base;
    private final JSONReader reader;


    JSONDecoder(final JSON json, final URI base, final Readable source) {
        this.base=base;
        this.reader=new JSONReader(source);
    }


    Frame decode(final Shape shape) {

        final Value value=object(shape);

        reader.token(EOF);

        if ( value instanceof Frame ) {

            return (Frame)value;

        } else {

            throw new CodecException("expected frame value", 1, 1);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Set<Value> values(final Shape shape) {
        switch ( reader.type() ) {

            case LBRACE:

                return shape.datatype()
                        .filter(RDF.LANGSTRING::equals)
                        .map(iri -> locals())
                        .orElseGet(() -> Set.of(object(shape)));

            case LBRACKET:

                return array(shape);


            case NULL:

                reader.token();

                return Set.of(NIL);

            case TRUE:

                reader.token();

                return Set.of(TRUE);

            case FALSE:

                reader.token();

                return Set.of(FALSE);

            case NUMBER:

                return Set.of(number(shape));

            case STRING:

                return Set.of(string(shape));


            default:

                return reader.error("unexpected %s", reader.type().description());

        }
    }

    private Value value(final Shape shape) {
        switch ( reader.type() ) {

            case LBRACE:

                return object(shape);


            case NULL:

                reader.token();

                return NIL;

            case TRUE:

                reader.token();

                return TRUE;

            case FALSE:

                reader.token();

                return FALSE;

            case NUMBER:

                return number(shape);

            case STRING:

                return string(shape);


            default:

                return reader.error("unexpected %s", reader.type().description());

        }
    }


    private Set<Value> array(final Shape shape) {

        final Set<Value> values=new LinkedHashSet<>();

        reader.token(LBRACKET);

        for (boolean tail=false; reader.type() != RBRACKET; tail=true) {

            if ( tail ) {
                reader.token(COMMA, RBRACKET);
            }

            values.add(value(shape));

        }

        reader.token(RBRACKET);

        return values;

    }

    private Value object(final Shape shape) {

        final Map<String, String> entries=new HashMap<>();

        final Collection<Field> fields=new ArrayList<>();
        final Collection<Query> queries=new ArrayList<>();

        reader.token(LBRACE);

        for (boolean tail=false; reader.type() != RBRACE; tail=true) {

            if ( tail ) {
                reader.token(COMMA, RBRACE);
            }

            final String label=reader.token(STRING);

            reader.token(COLON);

            try {

                if ( label.startsWith("<=") ) {

                    final Expression expression=expression(label.substring(2), shape);
                    final Value value=value(expression.apply(shape));

                    queries.add(filter(expression, lte(value)));

                } else if ( label.startsWith(">=") ) {

                    final Expression expression=expression(label.substring(2), shape);
                    final Value value=value(expression.apply(shape));

                    queries.add(filter(expression, gte(value)));

                } else if ( label.startsWith("<") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final Value value=value(expression.apply(shape));

                    queries.add(filter(expression, lt(value)));

                } else if ( label.startsWith(">") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final Value value=value(expression.apply(shape));

                    queries.add(filter(expression, gt(value)));

                } else if ( label.startsWith("~") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final String value=reader.token(STRING);

                    queries.add(filter(expression, like(value)));

                } else if ( label.startsWith("?") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final Collection<Value> values=values(expression.apply(shape));

                    queries.add(filter(expression, any(values)));

                } else if ( label.startsWith("^") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final int priority=priority(reader.token(STRING, NUMBER));

                    queries.add(order(expression, priority));

                } else if ( label.startsWith("$") ) {

                    final Expression expression=expression(label.substring(1), shape);
                    final Set<Value> values=values(expression.apply(shape));

                    queries.add(focus(expression, values));

                } else if ( label.equals("@") ) {

                    queries.add(offset(parseInt(reader.token(NUMBER))));

                } else if ( label.equals("#") ) {

                    queries.add(limit(parseInt(reader.token(NUMBER))));

                } else if ( label.startsWith("@") && !label.equals(_ID) ) {

                    if ( entries.put(label, reader.token(STRING)) != null ) {
                        return reader.error("duplicated special field <%s>", label);
                    }

                } else {

                    final Entry<IRI, Shape> entry=predicate(label, shape);

                    fields.add(field(entry.getKey(), values(entry.getValue())));

                }

            } catch ( final IllegalArgumentException e ) {

                return reader.error(e.getMessage());

            }

        }

        reader.token(RBRACE);


        if ( entries.isEmpty() ) {

            final Frame frame=frame(fields);

            return queries.isEmpty() ? frame : query(frame, queries);

        } else if ( fields.isEmpty() && queries.isEmpty() ) {

            final String value=entries.get(_VALUE);
            final String datatype=entries.get(_TYPE);
            final String language=entries.get(_LANGUAGE);

            return entries.keySet().equals(TYPED) ? literal(value, iri(datatype))
                    : entries.keySet().equals(TAGGED) ? literal(value, language)
                    : reader.error("malformed literal object");

        } else {

            return reader.error("malformed literal object");

        }

    }


    private Set<Value> locals() {

        final Set<Value> values=new LinkedHashSet<>();

        reader.token(LBRACE);

        for (boolean tail=false; reader.type() != RBRACE; tail=true) {

            if ( tail ) {
                reader.token(COMMA, RBRACE);
            }

            final String locale=reader.token(STRING);

            reader.token(COLON);

            switch ( reader.type() ) {

                case LBRACKET:

                    values.addAll(locals(locale));

                    break;

                case STRING:

                    values.add(literal(reader.token(STRING), locale));

                    break;

                default:

                    return reader.error("unexpected %s", reader.type().description());

            }

        }

        reader.token(RBRACE);

        return values;
    }

    private Set<Value> locals(final String locale) {

        final Set<Value> locals=new LinkedHashSet<>();

        reader.token(LBRACKET);

        for (boolean tail=false; reader.type() != RBRACKET; tail=true) {

            if ( tail ) {
                reader.token(COMMA, RBRACKET);
            }

            locals.add(literal(reader.token(STRING), locale));

        }

        reader.token(RBRACKET);

        return locals;
    }


    private Value number(final Shape shape) { // !!! cast result to expected numeric type

        final String token=reader.token(NUMBER);

        try {

            return token.indexOf('e') >= 0 ? literal(parseDouble(token))
                    : token.indexOf('E') >= 0 ? literal(parseDouble(token))
                    : token.indexOf('.') >= 0 ? literal(new BigDecimal(token))
                    : literal(new BigInteger(token));

        } catch ( final NumberFormatException e ) {

            return reader.error("malformed numeric value <%s>", token); // unexpected

        }
    }

    private Value string(final Shape shape) {

        final String token=reader.token(STRING);

        return _Parser.value(token, shape, base);
    }

}
