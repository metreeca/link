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

package com.metreeca.link.json;

import com.metreeca.link.Frame;
import com.metreeca.link.Query;
import com.metreeca.link.Shape;
import com.metreeca.link.Table;
import com.metreeca.link.Table.Column;
import com.metreeca.link.json.JSON.Decoder;
import com.metreeca.link.json.JSON.Encoder;
import com.metreeca.link.json.JSON.Type;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Query.Constraint.*;
import static com.metreeca.link.Query.*;
import static com.metreeca.link.Stash.Expression.alias;
import static com.metreeca.link.Stash.Expression.expression;
import static com.metreeca.link.Table.Column.column;
import static com.metreeca.link.json.JSON.Tokens.*;

import static java.lang.String.format;

final class TypeObject implements Type<Object> {

    @Override public void encode(final Encoder encoder, final Object value) throws IOException {

        final Frame<Object> frame=frame(value);

        if ( encoder.cache(frame) ) {

            encoder.open("{");

            boolean tail=false;

            final Iterable<Entry<String, Object>> entries=() -> frame.entries(true).iterator(); // ;( handle checked IOE

            for (final Entry<?, ?> entry : entries) {

                final String label=entry.getKey().toString();
                final Object object=entry.getValue();

                if ( object != null ) {

                    if ( tail ) {
                        encoder.comma();
                    }

                    encoder.indent();
                    encoder.encode(label);
                    encoder.colon();
                    encoder.encode(object);

                    tail=true;

                }

            }

            encoder.close("}", tail);

        } else {

            final String idField=frame.shape().id().orElse(null);
            final String idValue=frame.id();

            encoder.open("{");

            if ( idField != null && idValue != null ) {
                encoder.encode(idField);
                encoder.colon();
                encoder.encode(idValue);
            }

            encoder.close("}", false);

        }

    }

    @Override public Object decode(final Decoder decoder, final Class<Object> clazz) throws IOException {
        return clazz.equals(Object.class)
                ? object(decoder)  // no type information >> guess from input
                : object(decoder, clazz); // unhandled type >> introspect
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Object object(final Decoder decoder) throws IOException {
        switch ( decoder.type() ) {

            case LBRACE:

                return decoder.decode(Map.class);

            case LBRACKET:

                return decoder.decode(Collection.class);

            case STRING:

                return decoder.decode(String.class);

            case NUMBER:

                return decoder.decode(Number.class);

            case TRUE:
            case FALSE:

                return decoder.decode(Boolean.class);

            case NULL:

                return decoder.decode(Void.class);

            default:

                throw new RuntimeException(format(
                        "expected value, found %s", decoder.type().description()
                ));

        }
    }


    private Object object(final Decoder decoder, final Class<?> clazz) throws IOException {

        final Frame<?> frame=frame(clazz);
        final Shape shape=frame.shape();

        final Collection<Query<?>> queries=new ArrayList<>();
        final Map<String, Column> columns=new LinkedHashMap<>();

        decoder.token(LBRACE);

        for (boolean tail=false; decoder.type() != RBRACE; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            final String field=decoder.decode(String.class);

            decoder.token(COLON);

            final Query<?> query=query(decoder, field, shape);

            if ( query != null ) {

                queries.add(query);

            } else {

                final Map<String, Column> table=table(decoder, field, shape);

                if ( table != null ) {

                    columns.putAll(table);

                } else {

                    frame.set(field, value(decoder, shape.shape(field).orElseThrow(() ->
                            new RuntimeException(format("unknown object field <%s>", field))
                    )));

                }

            }

        }

        decoder.token(RBRACE);

        if ( !columns.isEmpty() ) {

            frame.entries(false).forEach(entry -> { // merge frame fields

                final String field=entry.getKey();
                final Object value=entry.getValue();

                if ( value != null ) {
                    columns.put(field, column(expression(List.of(field), List.of()), value));
                }

            });

            queries.add(model(Table.table(columns)));

            return Query.query(queries);

        } else if ( !queries.isEmpty() ) {

            queries.add(model(frame.value()));

            return Query.query(queries);

        } else {

            return frame.value();

        }

    }

    private Collection<?> array(final Decoder decoder, final Class<?> clazz) throws IOException {
        return array(decoder, clazz, false);
    }

    private Collection<?> array(final Decoder decoder, final Class<?> clazz, final boolean nullable) throws IOException {

        final List<Object> items=new ArrayList<>();

        decoder.token(LBRACKET);

        for (boolean tail=false; decoder.type() != RBRACKET; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            items.add(nullable && decoder.type() == NULL ? decoder.decode(Void.class) : decoder.decode(clazz));
        }

        decoder.token(RBRACKET);

        if ( items.stream().anyMatch(Query.class::isInstance) ) {

            if ( items.size() > 1 ) {
                throw new IllegalArgumentException("multiple collection queries");
            }

            return (Collection<?>)items.get(0);

        } else {

            return items;

        }

    }


    private Query<?> query(final Decoder decoder, final String constraint, final Shape shape) throws IOException {

        if ( constraint.startsWith("<=") ) {

            final Expression expression=expression(constraint.substring(2));
            final Object value=decoder.decode(shape.shape(expression).flatMap(Shape::clazz).orElse(Object.class));

            return filter(expression, lte(value));

        } else if ( constraint.startsWith(">=") ) {

            final Expression expression=expression(constraint.substring(2));
            final Object value=decoder.decode(shape.shape(expression).flatMap(Shape::clazz).orElse(Object.class));

            return filter(expression, gte(value));

        } else if ( constraint.startsWith("<") ) {

            final Expression expression=expression(constraint.substring(1));
            final Object value=decoder.decode(shape.shape(expression).flatMap(Shape::clazz).orElse(Object.class));

            return filter(expression, lt(value));

        } else if ( constraint.startsWith(">") ) {

            final Expression expression=expression(constraint.substring(1));
            final Object value=decoder.decode(shape.shape(expression).flatMap(Shape::clazz).orElse(Object.class));

            return filter(expression, gt(value));

        } else if ( constraint.startsWith("~") ) {

            final Expression expression=expression(constraint.substring(1));
            final String value=decoder.token(STRING);

            return filter(expression, like(value));

        } else if ( constraint.startsWith("?") ) {

            final Expression expression=expression(constraint.substring(1));
            final Collection<?> value=array(decoder, shape.shape(expression).flatMap(Shape::clazz).orElse(Object.class), true);

            return filter(expression, any(value));

        } else if ( constraint.equals("^") ) {

            final Collection<Query<?>> queries=new ArrayList<>();

            decoder.token(LBRACE);

            for (boolean tail=false; decoder.type() != RBRACE; tail=true) {

                if ( tail ) {
                    decoder.token(COMMA);
                }

                final Expression expression=expression(decoder.decode(String.class));

                decoder.token(COLON);

                final String direction=decoder.token(STRING);

                try {

                    queries.add(order(expression, Criterion.valueOf(direction)));

                } catch ( final IllegalArgumentException ignored ) {

                    throw new IllegalArgumentException(format("unknown sorting direction <%s>", direction));

                }

            }

            decoder.token(RBRACE);

            return Query.query(queries);

        } else if ( constraint.equals("$") ) {

            final Collection<Query<?>> queries=new ArrayList<>();

            decoder.token(LBRACE);

            for (boolean tail=false; decoder.type() != RBRACE; tail=true) {

                if ( tail ) {
                    decoder.token(COMMA);
                }

                final Expression expression=expression(decoder.decode(String.class));

                decoder.token(COLON);

                final Collection<?> values=array(decoder, Object.class, true);

                queries.add(focus(expression, new HashSet<>(values)));

            }

            decoder.token(RBRACE);

            return Query.query(queries);

        } else if ( constraint.equals("@") ) {

            return offset(Integer.parseInt(decoder.token(NUMBER)));

        } else if ( constraint.equals("#") ) {

            return limit(Integer.parseInt(decoder.token(NUMBER)));

        } else {

            return null;

        }

    }

    private Map<String, Column> table(final Decoder decoder, final String field, final Shape shape) throws IOException {

        final Optional<Entry<String, Expression>> alias=alias(field);

        if ( alias.isPresent() ) {

            final String name=alias.get().getKey();
            final Expression expression=alias.get().getValue();

            final Shape _shape=shape.shape(expression).orElse(null); // ; null+if to handle IOException

            final Object model=(_shape != null) ? value(decoder, _shape) : decoder.decode(Object.class);

            return Map.of(name, column(expression, model));

        } else {

            return null;

        }
    }


    private Object value(final Decoder decoder, final Shape shape) throws IOException {

        final Class<?> type=shape.clazz().orElse(Object.class);

        return shape.maxCount().filter(count -> count == 1).isPresent()
                ? decoder.decode(type)
                : array(decoder, type);
    }

}
