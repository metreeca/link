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

import java.io.IOException;
import java.time.OffsetDateTime;

import static com.metreeca.link.json.JSON.Tokens.STRING;

final class TypeOffsetDateTime implements Type<OffsetDateTime> {

    @Override public void encode(final Encoder encoder, final OffsetDateTime value) throws IOException {
        encoder.escape(value.toString());
    }

    @Override public OffsetDateTime decode(final Decoder decoder, final Class<OffsetDateTime> clazz) throws IOException {
        return OffsetDateTime.parse(decoder.token(STRING));
    }

}
