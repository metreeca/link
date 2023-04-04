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

package com.metreeca.rest.json;

import com.metreeca.rest.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.metreeca.rest.json.JSON.Tokens.EOF;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;

/**
 * JSON codec.
 */
public final class JSON implements Codec {

    /**
     * JSON token types.
     */
    public enum Tokens {

        LBRACE("object opening braces"),
        RBRACE("object closing brace"),

        LBRACKET("array opening bracket"),
        RBRACKET("array closing bracket"),

        STRING("string value"),
        NUMBER("number value"),

        TRUE("<true> literal"),
        FALSE("<false> literal"),
        NULL("<null> literal"),

        COLON("colon"),
        COMMA("comma"),

        EOF("end of input");


        private final String description;

        private Tokens(final String description) {
            this.description=description;
        }

        String description() {
            return description;
        }

    }


    public static JSON json() {
        return new JSON();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean pretty;

    private List<Entry<Class<?>, Type<?>>> types=List.of(

            entry(Void.class, new TypeVoid()),
            entry(Boolean.class, new TypeBoolean()),
            entry(Number.class, new TypeNumber()),
            entry(String.class, new TypeString()),
            entry(URI.class, new TypeURI()),

            entry(Table.class, new TypeTable()),
            entry(Trace.class, new TypeTrace()),

            entry(Collection.class, new TypeCollection()),
            entry(Map.class, new TypeMap()),
            entry(Object.class, new TypeObject())

    );


    private JSON() { }

    private JSON(final JSON json) {
        this.pretty=json.pretty;
        this.types=json.types;
    }


    public JSON pretty(final boolean pretty) {

        final JSON json=new JSON(this);

        json.pretty=pretty;

        return json;
    }

    public <T> JSON type(final Class<T> clazz, final Type<T> codec) {

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        final JSON json=new JSON(this);

        final Entry<Class<?>, Type<?>> entry=entry(clazz, codec);

        json.types=Stream.concat(Stream.of(entry), types.stream()).collect(toList());

        return json;
    }


    @SuppressWarnings("unchecked")
    private <T> Type<T> type(final Class<T> value) {
        return types.stream()

                .filter(entry -> entry.getKey().isAssignableFrom(value))
                .findFirst()
                .map(Entry::getValue)

                .map(codec -> ((Type<T>)codec))

                .orElseThrow(() -> new IllegalArgumentException(format(
                        "unsupported value type <%s>", value.getName()
                )));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String encode(final Object value) {
        try ( final StringWriter writer=new StringWriter() ) {

            return encode(writer, value).toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    public <T> T decode(final String json, final Class<T> clazz) {

        if ( json == null ) {
            throw new NullPointerException("null json");
        }

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        try ( final StringReader reader=new StringReader(json) ) {

            return decode(reader, clazz);

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    @Override public <A extends Appendable> A encode(final A target, final Object value) throws IOException {

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        final Encoder encoder=new Encoder(this, target);

        encoder.encode(value);

        return target;
    }

    @Override public <T> T decode(final Readable source, final Class<T> clazz) throws IOException {

        if ( source == null ) {
            throw new NullPointerException("null source");
        }

        if ( clazz == null ) {
            throw new NullPointerException("null class");
        }

        final Decoder decoder=new Decoder(this, source);

        final T value=decoder.decode(clazz);

        decoder.token(EOF);

        // !!! Decoder.decode() may silently return a stashed value (Query/Table): see TypeObject

        if ( value != null && !clazz.isInstance(value) ) {
            throw new JSON.Exception("unexpected query outside collection");
        }

        return value;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * JSON type codec.
     */
    public static interface Type<T> {

        public void encode(final Encoder encoder, final T value) throws IOException;

        public T decode(final Decoder decoder, final Class<T> clazz) throws IOException, Exception;

    }


    public static final class Decoder {

        private final JSON json;
        private final Lexer lexer;


        private Decoder(final JSON json, final Readable source) {
            this.json=json;
            this.lexer=new Lexer(source);
        }


        // !!! may silently return a stashed value (Query/Table): see TypeObject

        public <T> T decode(final Class<T> clazz) throws Exception, IOException {

            if ( clazz == null ) {
                throw new NullPointerException("null clazz");
            }

            try {

                return json.type(clazz).decode(this, clazz);

            } catch ( final JSON.Exception e ) {

                throw e;

            } catch ( final UncheckedIOException e ) {

                throw e.getCause();

            } catch ( final RuntimeException e ) {

                throw new Exception(e.getMessage(), lexer.line(), lexer.col(), e);

            }
        }


        public Tokens type() throws IOException {
            return lexer.type();
        }

        public String token() {
            return lexer.token();
        }

        public String token(final Tokens expected) throws IOException {

            final Tokens actual=type();

            if ( actual != expected ) {
                throw new Exception(
                        format("expected %s, found %s", expected.description(), actual.description()),
                        lexer.line(), lexer.col()
                );
            }

            return token();
        }

    }

    public static final class Encoder {

        private final JSON json;
        private final Appendable target;


        private int level;


        private Encoder(final JSON json, final Appendable target) {
            this.json=json;
            this.target=target;
        }


        @SuppressWarnings("unchecked")
        public void encode(final Object value) throws Exception, IOException {
            try {

                ((Type<Object>)(value == null
                        ? json.type(Void.class)
                        : json.type(value.getClass())
                )).encode(this, value);

            } catch ( final Exception e ) {

                throw e;

            } catch ( final RuntimeException e ) {


                throw e.getClass().equals(RuntimeException.class)
                        ? new Exception(e.getMessage())
                        : new Exception(e.getMessage(), e);

            }
        }


        public void open(final String string) throws IOException {

            ++level;

            write(string);
        }

        public void close(final String string, final boolean tail) throws IOException {

            --level;

            if ( json.pretty && tail ) {
                indent();
            }

            write(string);
        }

        public void indent() throws IOException {
            if ( json.pretty ) {

                target.append('\n');

                for (int i=0; i < level; ++i) {
                    target.append("  ");
                }

            }
        }


        public void comma() throws IOException {

            write(',');
        }

        public void colon() throws IOException {
            if ( json.pretty ) {

                write(": ");

            } else {

                write(':');

            }
        }


        public void write(final char c) throws IOException {
            target.append(c);
        }

        public void write(final String string) throws IOException {
            target.append(string);
        }

    }


    public static final class Exception extends RuntimeException {

        private static final long serialVersionUID=-1267685327499864471L;


        private final int line;
        private final int col;


        private Exception(final String message) {

            super(message);

            this.line=0;
            this.col=0;

        }

        private Exception(final String message, final Throwable cause) {

            super(message, cause);

            this.line=0;
            this.col=0;

        }

        private Exception(final String message, final int line, final int col) {

            super(format("(%d,%d) %s", line, col, message));

            this.line=line;
            this.col=col;

        }

        private Exception(final String message, final int line, final int col, final Throwable cause) {

            super(format("(%d,%d) %s", line, col, message), cause);

            this.line=line;
            this.col=col;

        }


        public int getLine() {
            return line;
        }

        public int getCol() {
            return col;
        }

    }

}
