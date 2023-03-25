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
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.metreeca.rest.json.JSON.Tokens.NUMBER;

import static java.lang.String.format;

final class TypeNumber implements Type<Number> {

    @Override public void encode(final Encoder encoder, final Number value) throws IOException {
        encoder.write(value.toString());
    }

    @Override public Number decode(final Decoder decoder, final Class<Number> clazz) throws IOException {

        final String token=decoder.token(NUMBER);

        try {

            return token.indexOf('E') >= 0 ? Double.parseDouble(token)
                    : token.indexOf('.') >= 0 ? new BigDecimal(token)
                    : new BigInteger(token);

        } catch ( final NumberFormatException e ) {

            throw new RuntimeException(format("malformed number value %s", token), e); // unexpected

        }
    }

}
