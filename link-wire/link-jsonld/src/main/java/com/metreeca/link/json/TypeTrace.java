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

import com.metreeca.link.Trace;
import com.metreeca.link.json.JSON.*;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;

final class TypeTrace implements Type<Trace> {

    @Override public void encode(final Encoder encoder, final Trace value) throws IOException {

        encoder.open("{");

        boolean tail=!value.errors().isEmpty();

        if ( tail ) {
            encoder.indent();
            encoder.encode("@errors");
            encoder.colon();
            encoder.encode(value.errors());
        }

        for (final Map.Entry<?, ?> entry : value.entries().entrySet()) {

            final String label=entry.getKey().toString();
            final Object object=entry.getValue();

            if ( object != null ) {

                if ( tail ) {
                    encoder.comma();
                }

                encoder.indent();
                encoder.encode(label);
                encoder.colon();
                encoder.encode(object);

                tail=true;

            }

        }

        encoder.close("}", tail);

    }

    @Override public Trace decode(final Decoder decoder, final Class<Trace> clazz) throws IOException {
        throw new UnsupportedOperationException(format("read-only type <%s>", Trace.class.getSimpleName()));
    }

}
