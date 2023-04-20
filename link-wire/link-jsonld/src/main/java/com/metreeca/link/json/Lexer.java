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

import java.io.IOException;
import java.nio.CharBuffer;

import static com.metreeca.link.json.JSON.Tokens;
import static com.metreeca.link.json.JSON.Tokens.*;

import static java.lang.String.format;

final class Lexer {

    private final Readable readable;

    private char last; // the last character read

    private int line=1;
    private int col;

    private boolean cr;

    private Tokens type;

    private final CharBuffer buffer=CharBuffer.allocate(1024).limit(0);
    private final StringBuilder token=new StringBuilder(100);


    Lexer(final Readable readable) {
        this.readable=readable;
    }


    Tokens type() throws JSONException, IOException {

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

                        throw new RuntimeException(format("unexpected character <%s>", toString(peek()))); // !!!

                }
            }

        }
    }

    String token() {

        final String value=token.toString();

        type=null;
        token.setLength(0);

        return value;
    }


    int line() {
        return line;
    }

    int col() {
        return col;
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

                    escape();

                    break;

                case '\0':

                    throw new RuntimeException("unexpected end of input in string");

                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u0007':
                case '\u0008':
                case '\u0009':
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

                    throw new RuntimeException(format("unexpected control character <%s> in string", toString(peek())));

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
        switch ( read() ) {

            case 't':

                token.append('\t');

                break;

            case 'r':

                token.append('\r');

                break;

            case 'n':

                token.append('\n');

                break;

            case 'u':

                token.append((char)(hex()<<12|hex()<<8|hex()<<4|hex()));

                break;

            default:

                throw new RuntimeException("illegal escape sequence");

        }
    }

    private int hex() throws IOException { // !!! check hex digits
        return Character.digit(read(), 16);
    }


    private void number() throws IOException {
        integer();
        fraction();
        exponent();
    }

    private void integer() throws IOException {

        if ( peek() == '-' ) {
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

                    throw new RuntimeException("expected decimal digit after leading 0");

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

                throw new RuntimeException("expected decimal digit");

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

                    col++;

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
        return c < '\u0020' ? format("^%c", c+'A') : format("%c", c);
    }

}
