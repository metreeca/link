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

package com.metreeca.link.rdf4j;

import com.metreeca.link.Local;
import com.metreeca.link.rdf4j.RDF4J.Type;
import com.metreeca.link.rdf4j.RDF4J.Writer;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.metreeca.link.Local.local;

import static java.util.Map.entry;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.*;

final class TypeLocal implements Type<Local<?>> {

    @Override public CompletableFuture<Optional<Local<?>>> lookup(final RDF4J.Reader reader, final Set<Value> values, final Local<?> model) {

        final Map<Locale, ?> entries=model.values();

        final boolean empty=entries.keySet().isEmpty();
        final boolean wild=entries.keySet().stream().anyMatch(Local.Wildcard::equals);
        final boolean unique=entries.values().stream().anyMatch(String.class::isInstance);

        final Set<String> locales=entries.keySet().stream()
                .map(Locale::toLanguageTag)
                .collect(toSet());

        final Stream<Literal> literals=values.stream()
                .filter(Value::isLiteral)
                .map(Literal.class::cast)
                .filter(v -> v.getLanguage().isPresent())
                .filter(v -> empty || wild || v.getLanguage().filter(locales::contains).isPresent());

        final Local<?> local=unique

                ?
                local(literals
                        .map(v -> local(v.getLanguage().get(), v.stringValue()))
                        .collect(toList())
                )

                :
                local(literals
                        .collect(groupingBy(v -> v.getLanguage().get(), mapping(Value::stringValue, toSet())))
                        .entrySet()
                        .stream()
                        .map(entry -> local(entry.getKey(), entry.getValue()))
                        .collect(toList())
                );

        return completedFuture(local.values().isEmpty() ? Optional.empty() : Optional.of(local));
    }


    @Override public Entry<Stream<Value>, Stream<Statement>> _encode(final Writer writer, final Local<?> value) {
        return entry(

                value.values().entrySet().stream().flatMap(entry -> {

                    final String locale=entry.getKey().toLanguageTag();
                    final Object values=entry.getValue();

                    if ( values instanceof String ) {

                        return Stream.of(writer.factory().createLiteral(((String)values), locale));

                    } else if ( values instanceof Collection ) {

                        return ((Collection<?>)values).stream()
                                .filter(String.class::isInstance) // expected
                                .map(v -> writer.factory().createLiteral(((String)v), locale));

                    } else {

                        return Stream.empty(); // unexpected
                    }

                }),

                Stream.empty()
        );
    }

}
