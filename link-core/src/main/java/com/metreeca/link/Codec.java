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

package com.metreeca.link;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wire format codec.
 */
public interface Codec {

    public default String encode(final Object value) {
        try ( final StringWriter writer=new StringWriter() ) {

            return encode(writer, value).toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    public default <T> T decode(final String json, final Class<T> clazz) {

        if ( json == null ) {
            throw new NullPointerException("null json");
        }

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        if ( json.startsWith("%7B") ) { // URLEncoded JSON

            return decode(URLDecoder.decode(json, UTF_8), clazz);

        } else if ( json.startsWith("e3") ) { // Base64 JSON

            return decode(new String(Base64.getDecoder().decode(json), UTF_8), clazz);

            // !!! } else if ( ??? ) { // search parameters

        } else {

            try ( final StringReader reader=new StringReader(json) ) {

                return decode(reader, clazz);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }
        }

    }


    public <A extends Appendable> A encode(final A target, final Object value) throws IOException;

    public <T> T decode(final Readable source, final Class<T> clazz) throws IOException;

}
