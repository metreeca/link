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

import com.metreeca.rest.json.JSON.*;

import java.io.IOException;

import static com.metreeca.rest.json.JSON.Tokens.STRING;

import static java.lang.String.format;

final class TypeString implements Type<String> {

    @Override public void encode(final Encoder encoder, final String value) throws IOException {

        encoder.write('"');

        for (int i=0, n=value.length(); i < n; ++i) {

            final char c=value.charAt(i);

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

                    encoder.write(format("\\u%04X", (int)c));

                    break;

                case '\b':

                    encoder.write("\\b");

                    break;

                case '\f':

                    encoder.write("\\f");

                    break;

                case '\n':

                    encoder.write("\\n");

                    break;

                case '\r':

                    encoder.write("\\r");

                    break;

                case '\t':

                    encoder.write("\\t");

                    break;

                case '"':

                    encoder.write("\\\"");

                    break;

                case '\\':

                    encoder.write("\\\\");

                    break;

                default:

                    encoder.write(c);

                    break;

            }
        }

        encoder.write('"');
    }

    @Override public String decode(final Decoder decoder, final Class<String> clazz) throws IOException {
        return decoder.token(STRING);
    }

}
