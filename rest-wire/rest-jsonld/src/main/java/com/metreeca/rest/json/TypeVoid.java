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

import static com.metreeca.rest.json.JSON.Tokens.NULL;

final class TypeVoid implements Type<Void> {

    @Override public void encode(final Encoder encoder, final Void value) throws IOException {
        encoder.write("null");
    }

    @Override public Void decode(final Decoder decoder, final Class<Void> clazz) throws IOException {

        decoder.token(NULL);

        return null;
    }

}
