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

import com.metreeca.link.json.JSON.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.metreeca.link.json.JSON.Tokens.*;

final class TypeMap implements Type<Map<?, ?>> {

    @Override public void encode(final Encoder encoder, final Map<?, ?> value) throws IOException {

        encoder.open("{");

        boolean tail=false;

        for (final Entry<?, ?> entry : value.entrySet()) {

            final String label=entry.getKey().toString();
            final Object object=entry.getValue();

            if ( tail ) {
                encoder.comma();
            }

            encoder.indent();
            encoder.encode(label);
            encoder.colon();
            encoder.encode(object);

            tail=true;

        }

        encoder.close("}", tail);

    }

    @Override public Map<?, ?> decode(final Decoder decoder, final Class<Map<?, ?>> clazz) throws IOException {

        final Map<String, Object> map=new LinkedHashMap<>();

        decoder.token(LBRACE);

        for (boolean tail=false; decoder.type() != RBRACE; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            final String label=decoder.decode(String.class);

            decoder.token(COLON);

            final Object value=decoder.decode(Object.class);

            map.put(label, value);
        }

        decoder.token(RBRACE);

        return map;

    }

}
