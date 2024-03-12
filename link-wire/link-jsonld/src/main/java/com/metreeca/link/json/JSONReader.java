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

import com.metreeca.link.CodecException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.metreeca.link.json.JSONReader.Type.*;

import static java.lang.String.format;

final class JSONReader {

    /**
     * JSON token types.
     */
    enum Type {

        LBRACE("object opening brace"),
        RBRACE("object closing brace"),

        LBRACKET("array opening bracket"),
        RBRACKET("array closing bracket"),

        STRING("string literal"),
        NUMBER("number literal"),

        TRUE("<true> literal"),
        FALSE("<false> literal"),
        NULL("<null> literal"),

        COLON("colon"),
        COMMA("comma"),

        EOF("end of input");


        private final String description;


        private Type(final String description) {
            this.description=description;
        }


        String description() {
            return description;
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Readable readable;

    private char last; // the last character read

    private int line;
    private int col;

    private boolean cr;

    private Type type;

    private final CharBuffer buffer=CharBuffer.allocate(1024).limit(0);
    private final StringBuilder token=new StringBuilder(100);


    JSONReader(final Readable readable) {
        this.readable=readable;
    }


    Type type() {
        try {

            if ( type != null ) {

                return type;

            } else {

                while ( true ) {
                    switch ( peek() ) {

                        case '{':

                            markup();

                            return (type=LBRACE);

                        case '}':

                            markup();

                            return (type=RBRACE);

                        case '[':

                            markup();

                            return (type=LBRACKET);

                        case ']':

                            markup();

                            return (type=RBRACKET);

                        case '"':

                            string();

                            return (type=STRING);

                        case '-':
                        case '+': // extension
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':

                            number();

                            return (type=NUMBER);

                        case 't':

                            literal("true");

                            return (type=TRUE);

                        case 'f':

                            literal("false");

                            return (type=FALSE);

                        case 'n':

                            literal("null");

                            return (type=NULL);

                        case ':':

                            markup();

                            return (type=COLON);

                        case ',':

                            markup();

                            return (type=COMMA);

                        case '\0':

                            return (type=EOF);

                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':

                            read();

                            break;

                        default:

                            return error("unexpected character <%s>", toString(peek()));

                    }
                }

            }

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    String token() {

        final String value=token.toString();

        type=null;
        token.setLength(0);

        return value;
    }

    String token(final Type expected) {

        final Type actual=type();

        return actual == expected ? token() : error("expected %s, found %s",
                expected.description(),
                actual.description()
        );

    }

    String token(final Type... expected) {

        final Type actual=type();

        return Arrays.stream(expected).anyMatch(t -> t == actual) ? token() : error("expected %s, found %s",
                Arrays.stream(expected).map(Type::description).collect(Collectors.joining(" or ")),
                actual.description()
        );

    }


    <T> T error(final String format, final Object... args) {
        throw new CodecException(format(format, args), line+1, type == EOF ? col+1 : col);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void markup() throws IOException {
        token.append(read());
    }


    private void string() throws IOException {

        read();

        while ( true ) {
            switch ( peek() ) {

                case '\\':

                    read();
                    escape();

                    break;

                case '\0':

                    error("unexpected end of input in string");

                    break;

                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u0007':
                case '\u0008':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                case '\r':
                case '\u000E':
                case '\u0010':
                case '\u0011':
                case '\u0012':
                case '\u0013':
                case '\u0014':
                case '\u0015':
                case '\u0016':
                case '\u0017':
                case '\u0018':
                case '\u0019':
                case '\u001A':
                case '\u001B':
                case '\u001C':
                case '\u001D':
                case '\u001E':
                case '\u001F':

                    error("unexpected control character <%s> in string", toString(peek()));

                    break;

                case '"':

                    read();

                    return;

                default:

                    token.append(read());

                    break;
            }
        }
    }

    private void escape() throws IOException {

        final char c=read();

        switch ( c ) {

            case '"':

                token.append('"');

                break;

            case '\\':

                token.append('\\');

                break;

            case '/':

                token.append('/');

                break;

            case 'b':

                token.append('\b');

                break;

            case 'f':

                token.append('\f');

                break;

            case 'r':

                token.append('\r');

                break;

            case 'n':

                token.append('\n');

                break;

            case 't':

                token.append('\t');

                break;

            case 'u':

                token.append((char)(hex() << 12|hex() << 8|hex() << 4|hex()));

                break;

            default:

                error("illegal escape sequence <\\%c>", c);

        }
    }

    private int hex() throws IOException {

        final char c=read();

        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                ? Character.digit(c, 16)
                : error("illegal digit in unicode escape sequence <%c>", c);
    }


    private void number() throws IOException {
        integer();
        fraction();
        exponent();
    }

    private void integer() throws IOException {

        if ( peek() == '-' || peek() == '+' ) {
            token.append(read());
        }

        if ( peek() == '0' ) {

            token.append(read());

            switch ( peek() ) {

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':

                    error("unexpected decimal digit after leading 0");

            }

        } else {

            digits();

        }
    }

    private void fraction() throws IOException {
        if ( peek() == '.' ) {

            token.append(read());

            digits();

        }
    }

    private void exponent() throws IOException {
        if ( peek() == 'e' || peek() == 'E' ) {

            token.append("E"); // normalize exponent case

            read();
            sign();
            digits();

        }
    }

    private void sign() throws IOException {
        if ( peek() == '-' || peek() == '+' ) {

            token.append(read());

        }
    }

    private void digits() throws IOException {

        switch ( peek() ) {

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

                token.append(read());

                break;

            default:

                error("expected decimal digit");

        }

        while ( true ) {
            switch ( peek() ) {

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':

                    token.append(read());

                    break;

                default:

                    return;

            }
        }
    }


    private void literal(final String literal) throws IOException {

        for (int i=0, n=literal.length(); i < n; ++i) {
            if ( read() != literal.charAt(i) ) {
                throw new RuntimeException(format("expected <%s> literal", literal));
            }
        }

        if ( peek() != '\0' && Character.isUnicodeIdentifierPart(peek()) ) {
            throw new RuntimeException(format("expected <%s> literal", literal));
        }

        token.append(literal);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private char peek() throws IOException {

        if ( last == '\0' ) {

            if ( !buffer.hasRemaining() ) {

                buffer.clear();

                final int n=readable.read(buffer);

                buffer.flip();

                if ( n < 0 ) {
                    return 0;
                }

            }

            last=buffer.get();

            if ( !cr && last == '\n' || (cr=(last == '\r')) ) {
                line++;
                col=0;
            } else {
                col++;
            }

        }

        return last;
    }

    private char read() throws IOException {

        final char c=peek();

        last='\0';

        return c;
    }


    private String toString(final char c) {
        return c < ' ' ? format("^%c", c+'A') : format("%c", c);
    }

}
