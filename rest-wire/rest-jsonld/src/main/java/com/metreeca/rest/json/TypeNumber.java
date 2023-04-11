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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.function.Function;

import static com.metreeca.rest.json.JSON.Tokens.NUMBER;

import static java.lang.String.format;
import static java.util.Locale.ROOT;

final class TypeNumber implements Type<Number> {

    private static final ThreadLocal<DecimalFormat> exponential=ThreadLocal.withInitial(() ->
            new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(ROOT)) // ;( not thread-safe
    );

    private static final Map<Class<?>, Function<String, Number>> parsers=Map.ofEntries(

            Map.entry(byte.class, Byte::valueOf),
            Map.entry(Byte.class, Byte::valueOf),

            Map.entry(short.class, Short::valueOf),
            Map.entry(Short.class, Short::valueOf),

            Map.entry(int.class, Integer::valueOf),
            Map.entry(Integer.class, Integer::valueOf),

            Map.entry(long.class, Long::valueOf),
            Map.entry(Long.class, Long::valueOf),

            Map.entry(float.class, Float::valueOf),
            Map.entry(Float.class, Float::valueOf),

            Map.entry(double.class, Double::valueOf),
            Map.entry(Double.class, Double::valueOf),

            Map.entry(BigInteger.class, BigInteger::new),
            Map.entry(BigDecimal.class, BigDecimal::new)

    );


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public void encode(final Encoder encoder, final Number value) throws IOException {
        encoder.write(
                value instanceof Double ? exponential.get().format(value)
                        : value instanceof Float ? exponential.get().format(value)
                        : value.toString()
        );
    }

    @Override public Number decode(final Decoder decoder, final Class<Number> clazz) throws IOException {

        final String token=decoder.token(NUMBER);

        try {

            return parsers.getOrDefault(clazz, string ->

                    string.indexOf('E') >= 0 ? Double.parseDouble(string)
                            : string.indexOf('.') >= 0 ? new BigDecimal(string)
                            : new BigInteger(string)

            ).apply(token);

        } catch ( final NumberFormatException e ) {

            throw new RuntimeException(format("malformed number value %s", token), e); // unexpected

        }
    }

}
