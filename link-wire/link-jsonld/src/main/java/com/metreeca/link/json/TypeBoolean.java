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

final class TypeBoolean implements Type<Boolean> {

    @Override public void encode(final Encoder encoder, final Boolean value) throws IOException {
        encoder.write(value.toString());
    }

    @Override public Boolean decode(final Decoder decoder, final Class<Boolean> clazz) throws IOException {
        switch ( decoder.type() ) {

            case FALSE:
            case TRUE:

                return Boolean.valueOf(decoder.token());

            default:

                throw new RuntimeException("expected boolean value");

        }
    }

}
