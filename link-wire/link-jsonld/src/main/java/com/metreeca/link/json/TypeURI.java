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

import com.metreeca.link.json.JSON.Decoder;
import com.metreeca.link.json.JSON.Type;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.metreeca.link.json.JSON.Tokens.STRING;

import static java.lang.String.format;

final class TypeURI implements Type<URI> {

    @Override public void encode(final com.metreeca.link.json.JSON.Encoder encoder, final URI value) throws IOException {
        encoder.escape(value.toString());
    }

    @Override public URI decode(final Decoder decoder, final Class<URI> clazz) throws IOException {

        final String token=decoder.token(STRING);

        try {

            return new URI(token);

        } catch ( final URISyntaxException e ) {

            throw new RuntimeException(format("malformed number value %s", token), e); // unexpected

        }
    }

}
