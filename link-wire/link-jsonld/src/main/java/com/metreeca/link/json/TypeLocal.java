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

import com.metreeca.link.Local;
import com.metreeca.link.json.JSON.Decoder;
import com.metreeca.link.json.JSON.Encoder;
import com.metreeca.link.json.JSON.Type;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static com.metreeca.link.Local.local;
import static com.metreeca.link.json.JSON.Tokens.*;

final class TypeLocal implements Type<Local<?>> {

    @Override public void encode(final Encoder encoder, final Local<?> value) throws IOException {

        encoder.open("{");

        boolean tail=false;

        for (final Entry<Locale, ?> entry : value.values().entrySet()) {

            final Locale locale=entry.getKey();
            final Object object=entry.getValue();

            if ( tail ) {
                encoder.comma();
            }

            encoder.indent();
            encoder.encode(locale.equals(Locale.ROOT) || locale.equals(Local.Wildcard) ? "" : locale.toLanguageTag());
            encoder.colon();
            encoder.encode(object);

            tail=true;

        }

        encoder.close("}", tail);
    }

    @Override public Local<?> decode(final Decoder decoder, final Class<Local<?>> clazz) throws IOException {

        final Collection<Local<String>> unique=new ArrayList<>();
        final Collection<Local<Set<String>>> common=new ArrayList<>();

        decoder.token(LBRACE);

        for (boolean tail=false; decoder.type() != RBRACE; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            final String target=decoder.decode(String.class);
            final Locale locale=target.equals("*") ? Local.Wildcard : Locale.forLanguageTag(target);

            decoder.token(COLON);

            if ( decoder.type() == LBRACKET ) {

                common.add(local(locale, values(decoder)));

            } else {

                unique.add(local(locale, decoder.decode(String.class)));

            }

        }

        decoder.token(RBRACE);

        if ( !unique.isEmpty() && !common.isEmpty() ) {
            throw new IllegalArgumentException("mixed unique and multiple localized values");
        }

        return !unique.isEmpty() ? local(unique)
                : !common.isEmpty() ? local(common)
                : local();

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Collection<String> values(final Decoder decoder) throws IOException {

        final Collection<String> values=new HashSet<>();

        decoder.token(LBRACKET);

        for (boolean tail=false; decoder.type() != RBRACKET; tail=true) {

            if ( tail ) {
                decoder.token(COMMA);
            }

            values.add(decoder.decode(String.class));
        }

        decoder.token(RBRACKET);

        return values;
    }

}
