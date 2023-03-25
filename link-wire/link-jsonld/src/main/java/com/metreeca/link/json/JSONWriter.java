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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

final class JSONWriter {

    private final boolean pretty;
    private final Appendable target;

    private final List<Boolean> stack=new ArrayList<>(List.of(false));


    JSONWriter(final JSON json, final Appendable target) {
        this.pretty=json.pretty();
        this.target=target;
    }


    void object(final boolean open) {
        if ( open ) { open('{'); } else { close('}'); }
    }

    void array(final boolean open) {
        if ( open ) { open('['); } else { close(']'); }
    }


    void colon() {
        if ( pretty ) {

            write(": ");

        } else {

            write(':');

        }
    }

    void comma() {

        if ( stack.get(0) ) {
            write(',');
            indent();
        }

    }


    void literal(final String literal) {

        if ( !stack.set(0, true) ) { indent(); }

        write(literal);

    }

    void string(final String string) {

        if ( !stack.set(0, true) ) { indent(); }

        write('"');

        for (int i=0, n=string.length(); i < n; ++i) {

            final char c=string.charAt(i);

            switch ( c ) {

                case '\u0000':
                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u0007':

                case '\u000B':

                case '\u000E':
                case '\u000F':
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

                    write(format("\\u%04X", (int)c));

                    break;

                case '\b':

                    write("\\b");

                    break;

                case '\f':

                    write("\\f");

                    break;

                case '\n':

                    write("\\n");

                    break;

                case '\r':

                    write("\\r");

                    break;

                case '\t':

                    write("\\t");

                    break;

                case '"':

                    write("\\\"");

                    break;

                case '\\':

                    write("\\\\");

                    break;

                default:

                    write(c);

                    break;

            }
        }

        write('"');

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void open(final char c) {

        stack.add(0, false);

        write(c);
    }

    private void close(final char c) {

        if ( stack.remove(0) ) {
            indent();
        }

        write(c);
    }


    private void indent() {
        if ( pretty ) {

            write('\n');

            for (int spaces=4*(stack.size()-1); spaces > 0; --spaces) {
                write(' ');
            }

        }
    }


    private void write(final char c) {
        try { target.append(c); } catch ( final IOException e ) { throw new UncheckedIOException(e); }
    }

    private void write(final String s) {
        try { target.append(s); } catch ( final IOException e ) { throw new UncheckedIOException(e); }
    }

}
