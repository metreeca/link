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
import com.metreeca.link.json.JSON.Encoder;
import com.metreeca.link.json.JSON.Type;
import com.metreeca.link.specs.Report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.metreeca.link.json.JSON.Tokens.*;

final class TypeCollection implements Type<Collection<?>> {

    @Override public void encode(final Encoder encoder, final Collection<?> value) throws IOException {

        if ( value instanceof Report ) {

            encoder.encode(((Report<?>)value).value());

        } else {

            encoder.open("[");

            boolean tail=false;

            for (final Object item : value) { // regular items

                if ( tail ) {
                    encoder.comma();
                }

                encoder.indent();
                encoder.encode(item);

                tail=true;

            }

            encoder.close("]", tail);

        }
    }

    @Override public Collection<?> decode(final Decoder decoder, final Class<Collection<?>> clazz) throws IOException {

        final Collection<Object> collection=Set.class.isAssignableFrom(clazz)
                ? new LinkedHashSet<>()
                : new ArrayList<>();

        decoder.token(LBRACKET);

        for (boolean tail=false; decoder.type() != RBRACKET; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            collection.add(decoder.decode(Object.class));
        }

        decoder.token(RBRACKET);

        return collection;
    }

}
