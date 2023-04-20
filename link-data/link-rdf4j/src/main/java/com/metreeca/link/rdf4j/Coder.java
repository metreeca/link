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

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * Source code generator.
 */
public abstract class Coder {

    public static Coder block(final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return items(text("\r{\t"), items(coders), text("\b }"));
    }

    public static Coder parens(final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return items(text("("), items(coders), text(")"));
    }


    public static Coder line(final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return items(text('\n'), items(coders), text('\n'));
    }

    public static Coder space(final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return items(text('\f'), items(coders), text('\f'));
    }

    public static Coder indent(final Coder... items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        return items(text('\t'), items(items), text('\b'));
    }


    public static Coder items(final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return items(List.of(coders));
    }

    public static Coder items(final Iterable<Coder> coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        return new Coder() {

            @Override public void generate(final Code code) {
                coders.forEach(item -> item.generate(code));
            }

        };
    }


    public static Coder list(final CharSequence separator, final Coder... coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        if ( separator == null ) {
            throw new NullPointerException("null separator");
        }

        return list(separator, asList(coders));
    }

    public static Coder list(final CharSequence separator, final Collection<Coder> coders) {

        if ( coders == null ) {
            throw new NullPointerException("null coders");
        }

        if ( separator == null ) {
            throw new NullPointerException("null separator");
        }

        return new Coder() {

            @Override public void generate(final Code code) {
                coders.stream()
                        .flatMap(item -> Stream.of(text(separator), item))
                        .skip(1)
                        .forEach(item -> item.generate(code));
            }

        };
    }


    public static Coder quoted(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return quoted(string, '\'');
    }

    public static Coder quoted(final String string, final char quote) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return new Coder() {

            @Override public void generate(final Code code) {

                code.append(quote);

                for (int i=0, n=string.length(); i < n; ++i) {

                    final char c=string.charAt(i);

                    switch ( c ) {

                        case '\\':

                            code.append("\\\\");
                            break;

                        case '\'':
                        case '\"':

                            code.append(c == quote ? "\\" : "");
                            code.append(c);
                            break;

                        case '\r':

                            code.append("\\r");
                            break;

                        case '\n':

                            code.append("\\n");
                            break;

                        case '\t':

                            code.append("\\t");
                            break;

                        default:

                            code.append(c);
                            break;

                    }
                }

                code.append(quote);

            }

        };
    }


    public static Coder text(final CharSequence... texts) {

        if ( texts == null ) {
            throw new NullPointerException("null text");
        }

        return new Coder() {

            @Override public void generate(final Code code) {
                for (final CharSequence text : texts) { code.append(text); }
            }

        };
    }

    public static Coder text(final CharSequence text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        return new Coder() {

            @Override public void generate(final Code code) {
                code.append(text);
            }

        };
    }

    public static Coder text(final char c) {
        return new Coder() {

            @Override public void generate(final Code code) {
                code.append(c);
            }

        };
    }


    public static Coder nothing() {
        return new Coder() {

            @Override public void generate(final Code code) { }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Coder() { }


    abstract void generate(final Code code);


    @Override public String toString() {
        return new Code().accept(this);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Code {

        private int indent; // indent level

        private char last; // last output
        private char wait; // pending optional whitespace

        private final StringBuilder builder=new StringBuilder(1024);


        private String accept(final Coder coder) {
            try {

                coder.generate(this);

                return builder.toString();

            } finally {

                builder.setLength(0);

            }
        }


        private void append(final CharSequence sequence) {

            for (int i=0, n=sequence.length(); i < n; ++i) { append(sequence.charAt(i)); }

        }

        private void append(final char c) {
            switch ( c ) {

                case '\f':

                    feed();
                    break;

                case '\r':

                    fold();
                    break;

                case '\n':

                    newline();
                    break;

                case ' ':

                    space();
                    break;

                case '\t':

                    indent();
                    break;

                case '\b':

                    outdent();
                    break;

                default:

                    other(c);
                    break;

            }
        }


        private void feed() {

            if ( last != '\0' ) { wait='\f'; }

        }

        private void fold() {

            if ( last != '\0' && wait != '\f' ) { wait=(wait == '\n') ? '\f' : ' '; }

        }

        private void newline() {

            if ( last != '\0' && wait != '\f' ) { wait='\n'; }

        }

        private void space() {

            if ( last != '\0' && wait != '\f' && wait != '\n' ) { wait=' '; }

        }


        private void indent() {

            if ( last != '\0' ) { ++indent; }

        }

        private void outdent() {

            if ( indent > 0 ) { --indent; }

        }


        private void other(final char c) {
            try {

                if ( wait == '\f' || wait == '\n' ) {

                    if ( last == '{' ) { ++indent; }

                    if ( c == '}' && indent > 0 ) { --indent; }

                    builder.append(wait == '\f' ? "\n\n" : "\n");

                    for (int i=indent; i > 0; --i) { builder.append("    "); }

                } else if ( wait == ' ' && last != '(' && c != ')' && last != '[' && c != ']' ) {

                    builder.append(' ');

                }

                builder.append(c);

            } finally {

                last=c;
                wait='\0';

            }
        }

    }

}
